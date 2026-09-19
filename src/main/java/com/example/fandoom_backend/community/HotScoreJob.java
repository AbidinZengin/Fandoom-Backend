package com.example.fandoom_backend.community;

import com.example.fandoom_backend.community.HotScoreRangeUpdater.IdRange;
import com.example.fandoom_backend.community.service.ThreadCacheNames;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * "hot" sıralaması için zaman-azalışlı skoru periyodik yeniden hesaplar:
 * {@code (like_count + 2*comment_count) / (yaşSaat + 2)^1.5} (bkz. ThreadRepository.recalculateHotScores).
 *
 * <p>Hesap ve yazma tamamen VERİTABANINDA yapılır: hiçbir thread Java'ya çekilmez (JVM belleği thread
 * sayısından bağımsız, sabit) ve satır başına ayrı UPDATE yoktur. Tek dev UPDATE yerine PK aralığı
 * pencereleriyle (varsayılan 10.000 id) çalışır: her pencere kısa bir transaction'dır, eşzamanlı
 * like/yorum sayaç güncellemelerini uzun süre kilitlemez. Pencere boyutu çok büyük verilirse tek
 * statement'a dönüşür.
 *
 * <p>UPDATE yalnızca hot_score kolonunu yazar: sayaçlar ezilmez, updatedAt değişmez. Bir pencere hata
 * verirse çalışma orada bırakılır (önceki pencereler commit'li; sonraki çalışma baştan hesaplar) ve liste
 * cache'i yine temizlenir.
 */
@Component
public class HotScoreJob {

    private static final Logger log = LoggerFactory.getLogger(HotScoreJob.class);

    private final HotScoreRangeUpdater rangeUpdater;
    private final long windowSize;

    public HotScoreJob(HotScoreRangeUpdater rangeUpdater,
                       @Value("${community.hot-score.window-size:10000}") long windowSize) {
        if (windowSize < 1) {
            throw new IllegalArgumentException("community.hot-score.window-size >= 1 olmalı: " + windowSize);
        }
        this.rangeUpdater = rangeUpdater;
        this.windowSize = windowSize;
    }

    // fixedDelay (fixedRate değil): önceki çalışma bittikten 15 dk SONRA — çalışma 15 dk'yı aşarsa
    // çalışmalar arka arkaya yığılmaz. hotScore DTO'da görünmez ama "hot" liste SIRASINI belirler:
    // çalışma sonunda liste cache'i temizlenir (etkin liste TTL'i böylece en fazla 15 dk). Hata yutulup
    // loglanır ki metod normal dönsün ve @CacheEvict her durumda çalışsın.
    @Scheduled(fixedDelay = 900000)
    @CacheEvict(cacheNames = ThreadCacheNames.LIST, allEntries = true)
    public void recalculateHotScores() {
        long startedAt = System.nanoTime();
        LocalDateTime now = LocalDateTime.now(); // tüm pencereler aynı "şimdi" ile hesaplanır (JVM saat dilimi:
        // created_at da aynı şekilde yazıldığından SQL'in NOW()'ı yerine parametre olarak verilir)
        long matched = 0;
        int windows = 0;
        try {
            Optional<IdRange> range = rangeUpdater.publishedIdRange();
            if (range.isEmpty()) {
                return;
            }
            long maxId = range.get().maxId();
            long from = range.get().minId();
            while (true) {
                long to = saturatedAdd(from, windowSize);
                matched += rangeUpdater.recalculate(from, to, now);
                windows++;
                if (to > maxId) {
                    break; // son pencere (to == Long.MAX_VALUE da buraya düşer: taşma/sonsuz döngü yok)
                }
                from = to;
            }
            log.info("HotScoreJob: {} thread eşleşti ({} pencere x {} id), {} ms",
                    matched, windows, windowSize, (System.nanoTime() - startedAt) / 1_000_000);
        } catch (RuntimeException e) {
            log.error("HotScoreJob: {} pencere sonra hata, kalan aralık bir sonraki çalışmaya kaldı (eşleşen={})",
                    windows, matched, e);
        }
    }

    private static long saturatedAdd(long a, long b) {
        long sum = a + b;
        return sum < a ? Long.MAX_VALUE : sum; // taşma -> tavan
    }
}
