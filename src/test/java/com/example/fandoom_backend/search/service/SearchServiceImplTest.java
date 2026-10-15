package com.example.fandoom_backend.search.service;

import com.example.fandoom_backend.blog.dto.BlogSearchResult;
import com.example.fandoom_backend.blog.service.BlogQueryService;
import com.example.fandoom_backend.common.dto.PageResponse;
import com.example.fandoom_backend.common.exception.InvalidReferenceException;
import com.example.fandoom_backend.community.dto.PortalSummaryResponse;
import com.example.fandoom_backend.movie.dto.MovieSummaryResponse;
import com.example.fandoom_backend.series.dto.SeriesSummaryResponse;
import org.springframework.data.domain.PageRequest;
import com.example.fandoom_backend.community.dto.PortalSearchResult;
import com.example.fandoom_backend.community.service.PortalQueryService;
import com.example.fandoom_backend.movie.dto.MovieSearchResult;
import com.example.fandoom_backend.movie.service.MovieService;
import com.example.fandoom_backend.search.dto.QuickSearchResponse;
import com.example.fandoom_backend.search.dto.SearchHit;
import com.example.fandoom_backend.search.dto.SearchType;
import com.example.fandoom_backend.search.mapper.SearchHitMapper;
import com.example.fandoom_backend.series.service.SeriesService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.i18n.LocaleContextHolder;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SearchServiceImplTest {

    private static final Duration TIMEOUT = Duration.ofMillis(200);

    @Mock private MovieService movieService;
    @Mock private SeriesService seriesService;
    @Mock private PortalQueryService portalQueryService;
    @Mock private BlogQueryService blogQueryService;

    private ExecutorService executor;
    private SearchServiceImpl service;

    @BeforeEach
    void setUp() {
        executor = Executors.newVirtualThreadPerTaskExecutor();
        cacheManager = new ConcurrentMapCacheManager();
        service = new SearchServiceImpl(movieService, seriesService, portalQueryService, blogQueryService,
                new SearchHitMapper(), executor, cacheManager, TIMEOUT);
    }

    private ConcurrentMapCacheManager cacheManager;

    @AfterEach
    void tearDown() {
        executor.shutdownNow();
        LocaleContextHolder.resetLocaleContext();
    }

    private void allSourcesEmpty(String q) {
        when(movieService.quickSearch(q, 5)).thenReturn(List.of());
        when(seriesService.quickSearch(q, 5)).thenReturn(List.of());
        when(portalQueryService.quickSearch(q, 5)).thenReturn(List.of());
        when(blogQueryService.quickSearch(q, 5)).thenReturn(List.of());
    }

    @Test
    void tooShortQuery_returnsEmpty_withoutTouchingAnySource() {
        assertThat(service.quickSearch(" d ").groups()).isEmpty();
        assertThat(service.quickSearch(null).groups()).isEmpty();
        verifyNoInteractions(movieService, seriesService, portalQueryService, blogQueryService);
    }

    @Test
    void tooLongQuery_isRejected() {
        assertThatThrownBy(() -> service.quickSearch("x".repeat(101))).isInstanceOf(InvalidReferenceException.class);
    }

    @Test
    void lengthLimit_appliesToTrimmedQuery() {
        String q = "x".repeat(100);
        allSourcesEmpty(q);

        assertThat(service.quickSearch("  " + q + "  ").groups()).isEmpty();

        verify(movieService).quickSearch(q, 5);
    }

    @Test
    void groupsFollowTypeOrder_emptyGroupsOmitted_titlesInRequestLanguage() {
        LocaleContextHolder.setLocale(Locale.forLanguageTag("tr"));
        when(movieService.quickSearch("dune", 5)).thenReturn(List.of(
                new MovieSearchResult(3L, "dune", "Dune", "Dune: Çöl Gezegeni", "poster", LocalDate.of(2021, 10, 22))));
        when(seriesService.quickSearch("dune", 5)).thenReturn(List.of());
        when(portalQueryService.quickSearch("dune", 5)).thenReturn(List.of(
                new PortalSearchResult(2L, "dune", "Dune Portalı", "Dune Portal", "icon")));
        when(blogQueryService.quickSearch("dune", 5)).thenReturn(List.of(
                new BlogSearchResult(8L, "dune-eco", "Dune ecology", "Dune ekolojisi", "Review", "İnceleme", "img")));

        QuickSearchResponse response = service.quickSearch(" dune ");

        assertThat(response.groups()).extracting(g -> g.type())
                .containsExactly(SearchType.MOVIE, SearchType.PORTAL, SearchType.BLOG);
        assertThat(response.groups().get(0).items())
                .containsExactly(new SearchHit(3L, "dune", "Dune: Çöl Gezegeni", "2021", "poster"));
        assertThat(response.groups().get(1).items())
                .containsExactly(new SearchHit(2L, "dune", "Dune Portalı", null, "icon"));
        assertThat(response.groups().get(2).items())
                .containsExactly(new SearchHit(8L, "dune-eco", "Dune ekolojisi", "İnceleme", "img"));
    }

    @Test
    void noMatchesAnywhere_isAComplete_emptyAnswer() {
        allSourcesEmpty("zzz");

        SearchServiceImpl.Gathered gathered = service.gather("zzz", Locale.ENGLISH);

        assertThat(gathered.response().groups()).isEmpty();
        assertThat(gathered.complete()).isTrue();
    }

    @Test
    void failingSource_leavesOnlyItsGroupEmpty_andMarksIncomplete() {
        when(movieService.quickSearch("dune", 5)).thenThrow(new IllegalStateException("db down"));
        when(seriesService.quickSearch("dune", 5)).thenReturn(List.of());
        when(portalQueryService.quickSearch("dune", 5)).thenReturn(List.of(
                new PortalSearchResult(2L, "dune", "Dune", "Dune", null)));
        when(blogQueryService.quickSearch("dune", 5)).thenReturn(List.of());

        SearchServiceImpl.Gathered gathered = service.gather("dune", Locale.ENGLISH);

        assertThat(gathered.response().groups()).extracting(g -> g.type()).containsExactly(SearchType.PORTAL);
        assertThat(gathered.complete()).isFalse();
    }

    @Test
    void completeAnswer_isCached_sameKeyIgnoresCaseAndSurroundingSpaces() {
        allSourcesEmpty("Dune");
        // Açıkça set edilmezse LocaleContextHolder JVM'in varsayılan dilini döndürür (bu makinede tr) -> anahtar tr:dune olurdu.
        LocaleContextHolder.setLocale(Locale.ENGLISH);

        service.quickSearch("Dune");
        service.quickSearch("  dune ");   // farklı yazım, aynı anahtar (en:dune) -> kaynaklara gidilmez
        service.quickSearch("DUNE");

        verify(movieService, times(1)).quickSearch("Dune", 5);
        assertThat(cacheManager.getCache(SearchCacheNames.QUICK).get("en:dune")).isNotNull();
    }

    @Test
    void languageIsPartOfTheKey() {
        allSourcesEmpty("dune");

        LocaleContextHolder.setLocale(Locale.forLanguageTag("tr"));
        service.quickSearch("dune");
        LocaleContextHolder.setLocale(Locale.ENGLISH);
        service.quickSearch("dune");

        verify(movieService, times(2)).quickSearch("dune", 5);
    }

    @Test
    void incompleteAnswer_isReturnedToUser_butNeverCached() {
        when(movieService.quickSearch("dune", 5)).thenThrow(new IllegalStateException("db down"));
        when(seriesService.quickSearch("dune", 5)).thenReturn(List.of());
        when(portalQueryService.quickSearch("dune", 5)).thenReturn(List.of(
                new PortalSearchResult(2L, "dune", "Dune", "Dune", null)));
        when(blogQueryService.quickSearch("dune", 5)).thenReturn(List.of());

        QuickSearchResponse first = service.quickSearch("dune");
        QuickSearchResponse second = service.quickSearch("dune");

        assertThat(first.groups()).extracting(g -> g.type()).containsExactly(SearchType.PORTAL);
        assertThat(second).isEqualTo(first);
        verify(portalQueryService, times(2)).quickSearch("dune", 5);   // ikinci istek de kaynaklara gitti
        assertThat(cacheManager.getCache(SearchCacheNames.QUICK).get("en:dune")).isNull();
    }

    @Test
    void fullSearch_shortQuery_returnsEmptyPage_withoutTouchingSource() {
        PageResponse<MovieSummaryResponse> page = service.searchMovies(" d ", 2, 20);

        assertThat(page.content()).isEmpty();
        assertThat(page.totalElements()).isZero();
        assertThat(page.page()).isEqualTo(2);
        assertThat(page.last()).isTrue();
        verifyNoInteractions(movieService);
    }

    @Test
    void fullSearch_tooLongQuery_isRejected() {
        assertThatThrownBy(() -> service.searchBlogs("x".repeat(101), 0, 20))
                .isInstanceOf(InvalidReferenceException.class);
    }

    @Test
    void fullSearch_trimsQuery_clampsPaging_andNeverForwardsASort() {
        PageResponse<SeriesSummaryResponse> expected = new PageResponse<>(List.of(), 0, 50, 0, 0, true);
        when(seriesService.fullSearch("dune", PageRequest.of(0, 50))).thenReturn(expected);

        assertThat(service.searchSeries("  dune ", -3, 500)).isSameAs(expected);
    }

    @Test
    void fullSearch_portals_passesViewerForIsMember() {
        PageResponse<PortalSummaryResponse> expected = new PageResponse<>(List.of(), 1, 1, 0, 0, true);
        when(portalQueryService.fullSearch("dune", 42L, PageRequest.of(1, 1))).thenReturn(expected);

        assertThat(service.searchPortals("dune", 42L, 1, 0)).isSameAs(expected);
    }

    @Test
    void slowSource_isAbandonedAfterTimeout_andMarksIncomplete() {
        when(movieService.quickSearch("dune", 5)).thenAnswer(inv -> {
            Thread.sleep(5_000);
            return List.of();
        });
        when(seriesService.quickSearch("dune", 5)).thenReturn(List.of(
                new com.example.fandoom_backend.series.dto.SeriesSearchResult(
                        5L, "dune-prophecy", "Dune: Prophecy", null, null, null)));
        when(portalQueryService.quickSearch("dune", 5)).thenReturn(List.of());
        when(blogQueryService.quickSearch("dune", 5)).thenReturn(List.of());

        long start = System.nanoTime();
        SearchServiceImpl.Gathered gathered = service.gather("dune", Locale.ENGLISH);
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;

        assertThat(elapsedMs).isLessThan(2_000);
        assertThat(gathered.response().groups()).extracting(g -> g.type()).containsExactly(SearchType.SERIES);
        assertThat(gathered.complete()).isFalse();
    }
}
