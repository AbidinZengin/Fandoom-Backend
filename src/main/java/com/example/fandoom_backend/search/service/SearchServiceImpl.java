package com.example.fandoom_backend.search.service;

import com.example.fandoom_backend.blog.service.BlogQueryService;
import com.example.fandoom_backend.common.exception.InvalidReferenceException;
import com.example.fandoom_backend.community.service.PortalQueryService;
import com.example.fandoom_backend.movie.service.MovieService;
import com.example.fandoom_backend.search.dto.QuickSearchResponse;
import com.example.fandoom_backend.search.dto.SearchGroup;
import com.example.fandoom_backend.search.dto.SearchHit;
import com.example.fandoom_backend.search.dto.SearchType;
import com.example.fandoom_backend.search.mapper.SearchHitMapper;
import com.example.fandoom_backend.series.service.SeriesService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.function.Supplier;

// "Danışma masası": modüllere yalnızca servis interface'leri üzerinden gider (Karar 1), 4 kaynağı paralel sorgular
// (scatter-gather, Karar 8), zamanında dönmeyeni boş bırakır, sonuçları istek dilinde SearchHit'e çevirir (Karar 7),
// yalnızca tam cevapları cache'ler (Karar 9).
// Tasarım: docs/plans/2026-10-15-unified-search-design.md
@Slf4j
@Service
public class SearchServiceImpl implements SearchService {

    static final int MIN_QUERY_LENGTH = 2;
    static final int MAX_QUERY_LENGTH = 100;
    static final int QUICK_LIMIT = 5;

    private final MovieService movieService;
    private final SeriesService seriesService;
    private final PortalQueryService portalQueryService;
    private final BlogQueryService blogQueryService;
    private final SearchHitMapper mapper;
    private final ExecutorService executor;
    private final CacheManager cacheManager;
    private final Duration timeout;

    public SearchServiceImpl(MovieService movieService, SeriesService seriesService,
                             PortalQueryService portalQueryService, BlogQueryService blogQueryService,
                             SearchHitMapper mapper, @Qualifier("searchExecutor") ExecutorService executor,
                             CacheManager cacheManager, @Value("${search.quick.timeout:1s}") Duration timeout) {
        this.movieService = movieService;
        this.seriesService = seriesService;
        this.portalQueryService = portalQueryService;
        this.blogQueryService = blogQueryService;
        this.mapper = mapper;
        this.executor = executor;
        this.cacheManager = cacheManager;
        this.timeout = timeout;
    }

    @Override
    public QuickSearchResponse quickSearch(String rawQuery) {
        String query = normalize(rawQuery);
        if (query == null) {
            return QuickSearchResponse.empty();
        }
        Locale locale = LocaleContextHolder.getLocale();
        Cache cache = cacheManager.getCache(SearchCacheNames.QUICK);
        if (cache == null) {
            return gather(query, locale).response();
        }
        // @Cacheable yerine elle: sync=true (stampede koruması) ile "eksik cevabı yazma" (unless) birlikte
        // kullanılamıyor. Cache.get(key, loader) sync yolunun aynısı: aynı anahtarda eşzamanlı miss'lerde loader'ı
        // tek çağrı çalıştırır (JVM-yerel kilit), loader exception fırlatırsa HİÇBİR ŞEY yazılmaz. Eksik cevap bu yüzden
        // IncompleteResultException ile "itiraz" edilir; içindeki kısmi cevap kullanıcıya yine döner.
        // Redis erişilemezse FailSafeCache loader'ı doğrudan çalıştırır (aynı itiraz yolu geçerli).
        try {
            return cache.get(cacheKey(query, locale), () -> {
                Gathered gathered = gather(query, locale);
                if (!gathered.complete()) {
                    throw new IncompleteResultException(gathered.response());
                }
                return gathered.response();
            });
        } catch (Cache.ValueRetrievalException e) {
            if (e.getCause() instanceof IncompleteResultException incomplete) {
                return incomplete.partial;
            }
            if (e.getCause() instanceof RuntimeException cause) {
                throw cause;
            }
            throw e;
        }
    }

    // "dil:küçük harfli metin". Cevabı değiştiren her şey anahtarda: dil (gösterilen başlık) + metin. Küçük harf yalnız
    // tutarlılık için (Locale.ROOT); DB eşleşmesi zaten harfe duyarsız olduğundan "Dune" ve "dune" aynı cevabı alır.
    static String cacheKey(String query, Locale locale) {
        String language = "tr".equalsIgnoreCase(locale.getLanguage()) ? "tr" : "en";
        return language + ":" + query.toLowerCase(Locale.ROOT);
    }

    // Akış kontrolü için exception: "bu cevabı saklama" sinyali (hata değil). Stack trace üretmez (ucuz).
    static final class IncompleteResultException extends RuntimeException {
        private final transient QuickSearchResponse partial;

        IncompleteResultException(QuickSearchResponse partial) {
            super("Quick search cevabı eksik, cache'e yazılmadı", null, false, false);
            this.partial = partial;
        }
    }

    // null = aranmayacak kadar kısa. Uzunluk kırpılmış hâle bakar.
    static String normalize(String rawQuery) {
        String trimmed = rawQuery == null ? "" : rawQuery.trim();
        if (trimmed.length() > MAX_QUERY_LENGTH) {
            throw new InvalidReferenceException("q en fazla " + MAX_QUERY_LENGTH + " karakter olabilir");
        }
        return trimmed.length() < MIN_QUERY_LENGTH ? null : trimmed;
    }

    // complete=false: en az bir kaynak zamanında dönmedi ya da hata verdi ("getirilemedi"). Boş ama dönmüş kaynak
    // ("bulunamadı") tamdır. Yalnızca tam cevaplar cache'e yazılır.
    record Gathered(QuickSearchResponse response, boolean complete) {}

    // locale çağıran tarafından isteğin kendi thread'inde okunup verilir; paralel görevler dil bilmez, ham iki dili döndürür.
    Gathered gather(String query, Locale locale) {
        var movies = submit(() -> movieService.quickSearch(query, QUICK_LIMIT));
        var series = submit(() -> seriesService.quickSearch(query, QUICK_LIMIT));
        var portals = submit(() -> portalQueryService.quickSearch(query, QUICK_LIMIT));
        var blogs = submit(() -> blogQueryService.quickSearch(query, QUICK_LIMIT));

        Waiter waiter = new Waiter(timeout);
        List<SearchGroup> groups = new ArrayList<>(SearchType.values().length);
        addGroup(groups, SearchType.MOVIE, waiter.await(SearchType.MOVIE, movies), m -> mapper.fromMovie(m, locale));
        addGroup(groups, SearchType.SERIES, waiter.await(SearchType.SERIES, series), s -> mapper.fromSeries(s, locale));
        addGroup(groups, SearchType.PORTAL, waiter.await(SearchType.PORTAL, portals), p -> mapper.fromPortal(p, locale));
        addGroup(groups, SearchType.BLOG, waiter.await(SearchType.BLOG, blogs), b -> mapper.fromBlog(b, locale));
        return new Gathered(new QuickSearchResponse(List.copyOf(groups)), waiter.complete);
    }

    private <T> CompletableFuture<List<T>> submit(Supplier<List<T>> task) {
        return CompletableFuture.supplyAsync(task, executor);
    }

    private static <T> void addGroup(List<SearchGroup> groups, SearchType type, List<T> results,
                                     Function<T, SearchHit> toHit) {
        if (!results.isEmpty()) {
            groups.add(new SearchGroup(type, results.stream().map(toHit).toList()));
        }
    }

    // Tüm kaynaklar için TEK ortak son tarih: kaynaklar sırayla beklense de toplam bekleme en fazla `timeout` olur.
    // Zaman aşımı yalnızca beklemeyi bırakır; arka plandaki DB sorgusu bitene kadar çalışmaya (ve bağlantıyı tutmaya)
    // devam eder — gerçek iptal için ayrıca DB sorgu zaman aşımı gerekir (tasarım belgesi, sonraya bırakılanlar).
    private static final class Waiter {
        private final long deadlineNanos;
        private final Duration timeout;
        private boolean complete = true;

        Waiter(Duration timeout) {
            this.timeout = timeout;
            this.deadlineNanos = System.nanoTime() + timeout.toNanos();
        }

        <T> List<T> await(SearchType type, CompletableFuture<List<T>> future) {
            long remaining = Math.max(0, deadlineNanos - System.nanoTime());
            try {
                return future.get(remaining, TimeUnit.NANOSECONDS);
            } catch (TimeoutException e) {
                complete = false;
                log.warn("Quick search: {} kaynağı {} ms içinde dönmedi, grup boş bırakıldı", type, timeout.toMillis());
            } catch (ExecutionException e) {
                complete = false;
                log.warn("Quick search: {} kaynağı hata verdi, grup boş bırakıldı", type, e.getCause());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                complete = false;
            }
            return List.of();
        }
    }
}
