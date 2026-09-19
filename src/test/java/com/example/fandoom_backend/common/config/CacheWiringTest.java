package com.example.fandoom_backend.common.config;

import com.example.fandoom_backend.blog.dto.BlogDetailResponse;
import com.example.fandoom_backend.blog.service.BlogDetailCache;
import com.example.fandoom_backend.community.dto.ThreadDetailResponse;
import com.example.fandoom_backend.community.entity.Thread;
import com.example.fandoom_backend.community.entity.ThreadStatus;
import com.example.fandoom_backend.community.mapper.ThreadMapper;
import com.example.fandoom_backend.community.repository.ThreadBookmarkRepository;
import com.example.fandoom_backend.community.repository.ThreadLikeRepository;
import com.example.fandoom_backend.community.repository.ThreadRepository;
import com.example.fandoom_backend.community.repository.ThreadTagRepository;
import com.example.fandoom_backend.community.service.ThreadServiceImpl;
import com.example.fandoom_backend.community.service.UserProfileService;
import com.example.fandoom_backend.franchise.service.FranchiseService;
import com.example.fandoom_backend.genre.service.GenreService;
import com.example.fandoom_backend.media.service.ImageStorageService;
import com.example.fandoom_backend.movie.dto.MovieDetailResponse;
import com.example.fandoom_backend.movie.entity.Movie;
import com.example.fandoom_backend.movie.mapper.MovieMapper;
import com.example.fandoom_backend.movie.repository.MovieRepository;
import com.example.fandoom_backend.movie.service.MovieService;
import com.example.fandoom_backend.movie.service.MovieServiceImpl;
import com.example.fandoom_backend.person.service.PersonService;
import com.example.fandoom_backend.series.service.SeriesService;
import com.example.fandoom_backend.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// @Cacheable/@CacheEvict annotation'larının (özellikle SpEL key/condition ifadelerinin, ki bunlar
// yalnızca çağrı anında değerlendirilir) gerçek Spring proxy'si üzerinden doğru çalıştığını kanıtlar.
// Redis yerine ConcurrentMapCacheManager: davranış (hit/miss/evict/condition) aynı, altyapı değişik.
@SpringJUnitConfig(CacheWiringTest.Cfg.class)
class CacheWiringTest {

    @Configuration
    @EnableCaching(proxyTargetClass = true)
    static class Cfg {
        @Bean CacheManager cacheManager() { return new ConcurrentMapCacheManager(); }

        @Bean ThreadRepository threadRepository() { return mock(ThreadRepository.class); }
        @Bean MovieRepository movieRepository() { return mock(MovieRepository.class); }
        @Bean MovieMapper movieMapper() { return mock(MovieMapper.class); }
        @Bean ThreadMapper threadMapper() { return mock(ThreadMapper.class); }
        @Bean BlogDetailCache blogDetailCache() { return new BlogDetailCache(); }

        @Bean ThreadServiceImpl threadService(ThreadRepository repo, ThreadMapper mapper) {
            UserService userService = mock(UserService.class);
            when(userService.getUsernamesByIds(any())).thenReturn(Map.of());
            UserProfileService profileService = mock(UserProfileService.class);
            when(profileService.getAvatarUrlsByUserIds(any())).thenReturn(Map.of());
            return new ThreadServiceImpl(repo, mock(ThreadTagRepository.class), mock(ThreadLikeRepository.class),
                    mock(ThreadBookmarkRepository.class), mapper, mock(com.example.fandoom_backend.movie.service.MovieService.class),
                    mock(SeriesService.class), userService, profileService,
                    mock(com.example.fandoom_backend.community.service.ThreadMediaService.class));
        }

        @Bean MovieServiceImpl movieService(MovieRepository repo, MovieMapper mapper) {
            return new MovieServiceImpl(repo, mapper, mock(FranchiseService.class), mock(GenreService.class),
                    mock(PersonService.class), mock(ImageStorageService.class));
        }
    }

    @Autowired private ThreadServiceImpl threadService;
    @Autowired private MovieServiceImpl movieService;
    @Autowired private BlogDetailCache blogDetailCache;
    @Autowired private ThreadRepository threadRepository;
    @Autowired private MovieRepository movieRepository;
    @Autowired private ThreadMapper threadMapper;
    @Autowired private MovieMapper movieMapper;
    @Autowired private CacheManager cacheManager;

    @BeforeEach
    void reset() {
        cacheManager.getCacheNames().forEach(n -> cacheManager.getCache(n).clear());
        clearInvocations(threadRepository, movieRepository, threadMapper, movieMapper);
        Thread thread = Thread.builder().id(10L).slug("s").status(ThreadStatus.PUBLISHED).authorId(1L).build();
        when(threadRepository.findBySlugAndStatus("s", ThreadStatus.PUBLISHED)).thenReturn(Optional.of(thread));
        when(threadRepository.findBySlug("s")).thenReturn(Optional.of(thread));
        when(threadMapper.toDetailResponse(any(), any(), any(), org.mockito.ArgumentMatchers.anyBoolean(),
                org.mockito.ArgumentMatchers.anyBoolean(), any())).thenReturn(mock(ThreadDetailResponse.class));
        when(threadRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class),
                any(org.springframework.data.domain.Pageable.class)))
                .thenAnswer(inv -> new PageImpl<>(List.<Thread>of(), inv.getArgument(1), 0));
        when(movieRepository.findById(1L)).thenReturn(Optional.of(Movie.builder().id(1L).build()));
        when(movieMapper.toDetailResponse(any())).thenReturn(mock(MovieDetailResponse.class));
        when(movieRepository.findAll(any(org.springframework.data.domain.Pageable.class)))
                .thenAnswer(inv -> new PageImpl<>(List.<Movie>of(), inv.getArgument(0), 0));
    }

    // ---- Thread ----

    @Test
    void thread_getBySlug_anonymousIsCached_authenticatedBypassesCache() {
        threadService.getBySlug("s", null);
        threadService.getBySlug("s", null);
        verify(threadRepository, times(1)).findBySlugAndStatus("s", ThreadStatus.PUBLISHED);

        // Giriş yapmış kullanıcının isLiked/isBookmarked'ı paylaşılan cache'e girmemeli.
        threadService.getBySlug("s", 5L);
        threadService.getBySlug("s", 5L);
        verify(threadRepository, times(3)).findBySlugAndStatus("s", ThreadStatus.PUBLISHED);
    }

    @Test
    void thread_list_sortIsNormalizedIntoKey_andDeepPagesAreNotCached() {
        var page0 = PageRequest.of(0, 20);
        threadService.list(null, null, null, "hot", null, page0);
        threadService.list(null, null, null, "garbage-sort", null, page0); // geçersiz sort -> "hot" ile AYNI key
        verify(threadRepository, times(1)).findAll(any(org.springframework.data.jpa.domain.Specification.class), any(org.springframework.data.domain.Pageable.class));

        var deep = PageRequest.of(7, 20);
        threadService.list(null, null, null, "hot", null, deep);
        threadService.list(null, null, null, "hot", null, deep);
        verify(threadRepository, times(3)).findAll(any(org.springframework.data.jpa.domain.Specification.class), any(org.springframework.data.domain.Pageable.class));
    }

    @Test
    void thread_cacheKey_normalizesTagsAndSort() {
        var p = PageRequest.of(0, 20);
        assertThat(com.example.fandoom_backend.community.service.ThreadCacheKeys.list(
                null, null, List.of("Theory", "s1"), "hot", p))
                .isEqualTo(com.example.fandoom_backend.community.service.ThreadCacheKeys.list(
                        null, null, List.of("s1", "theory"), "rastgele", p));
    }

    @Test
    void thread_delete_evictsDetailCache() {
        threadService.getBySlug("s", null);
        threadService.delete(1L, true, "s"); // moderatör
        threadService.getBySlug("s", null);
        verify(threadRepository, times(2)).findBySlugAndStatus("s", ThreadStatus.PUBLISHED);
    }

    // ---- Movie ----

    @Test
    void movie_getById_isCached_andWriteEvictsIt() {
        movieService.getById(1L);
        movieService.getById(1L);
        verify(movieRepository, times(1)).findById(1L);

        movieService.createBatch(List.of()); // yazma yolu (boş batch: yalnızca @CacheEvict davranışı sınanır)
        movieService.getById(1L);
        verify(movieRepository, times(2)).findById(1L);
    }

    @Test
    void movie_list_isCachedPerPage() {
        movieService.list(PageRequest.of(0, 20));
        movieService.list(PageRequest.of(0, 20));
        verify(movieRepository, times(1)).findAll(any(org.springframework.data.domain.Pageable.class));

        movieService.list(PageRequest.of(1, 20));
        verify(movieRepository, times(2)).findAll(any(org.springframework.data.domain.Pageable.class));
    }

    // ---- Blog detay cache'i (yan etkisiz kısım) ----

    @Test
    void blogDetailCache_invokesLoaderOncePerSlug() {
        AtomicInteger loads = new AtomicInteger();
        BlogDetailResponse detail = mock(BlogDetailResponse.class);

        blogDetailCache.getOrLoad("slug-a", () -> { loads.incrementAndGet(); return detail; });
        blogDetailCache.getOrLoad("slug-a", () -> { loads.incrementAndGet(); return detail; });
        blogDetailCache.getOrLoad("slug-b", () -> { loads.incrementAndGet(); return detail; });

        assertThat(loads.get()).isEqualTo(2);
    }
}
