package com.example.fandoom_backend.community.service;

import com.example.fandoom_backend.community.dto.ThreadBookmarkStatusResponse;
import com.example.fandoom_backend.community.dto.ThreadLikeStatusResponse;
import com.example.fandoom_backend.community.entity.Thread;
import com.example.fandoom_backend.community.entity.ThreadStatus;
import com.example.fandoom_backend.community.repository.ThreadBookmarkRepository;
import com.example.fandoom_backend.community.repository.ThreadLikeRepository;
import com.example.fandoom_backend.community.repository.ThreadRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// like/unlike/bookmark/unbookmark idempotent olmalı: zaten like'lı bir şeye
// tekrar like çağrılırsa save/incrementLikeCount ÇAĞRILMAZ ama response yine
// de liked=true döner (aynısı ters yönde unlike için).
@ExtendWith(MockitoExtension.class)
class ThreadInteractionServiceImplTest {

    private static final Long USER_ID = 1L;
    private static final Long THREAD_ID = 10L;
    private static final String SLUG = "slug";

    @Mock private ThreadRepository threadRepository;
    @Mock private ThreadLikeRepository threadLikeRepository;
    @Mock private ThreadBookmarkRepository threadBookmarkRepository;

    private ThreadInteractionServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ThreadInteractionServiceImpl(threadRepository, threadLikeRepository, threadBookmarkRepository);
    }

    private Thread threadWith(int likeCount, int bookmarkCount) {
        Thread thread = Thread.builder().id(THREAD_ID).slug(SLUG).status(ThreadStatus.PUBLISHED)
                .likeCount(likeCount).bookmarkCount(bookmarkCount).build();
        when(threadRepository.findBySlugAndStatus(SLUG, ThreadStatus.PUBLISHED)).thenReturn(Optional.of(thread));
        return thread;
    }

    @Test
    void like_notAlreadyLiked_savesAndIncrementsCount() {
        threadWith(5, 0);
        when(threadLikeRepository.existsByUserIdAndThreadId(USER_ID, THREAD_ID)).thenReturn(false);

        ThreadLikeStatusResponse response = service.like(USER_ID, SLUG);

        assertThat(response.liked()).isTrue();
        assertThat(response.likeCount()).isEqualTo(6);
        verify(threadLikeRepository).save(org.mockito.ArgumentMatchers.any());
        verify(threadRepository).incrementLikeCount(THREAD_ID);
    }

    @Test
    void like_alreadyLiked_isIdempotent() {
        threadWith(5, 0);
        when(threadLikeRepository.existsByUserIdAndThreadId(USER_ID, THREAD_ID)).thenReturn(true);

        ThreadLikeStatusResponse response = service.like(USER_ID, SLUG);

        assertThat(response.liked()).isTrue();
        assertThat(response.likeCount()).isEqualTo(5);
        verify(threadLikeRepository, never()).save(org.mockito.ArgumentMatchers.any());
        verify(threadRepository, never()).incrementLikeCount(THREAD_ID);
    }

    @Test
    void unlike_wasLiked_deletesAndDecrementsCount() {
        threadWith(5, 0);
        when(threadLikeRepository.existsByUserIdAndThreadId(USER_ID, THREAD_ID)).thenReturn(true);

        ThreadLikeStatusResponse response = service.unlike(USER_ID, SLUG);

        assertThat(response.liked()).isFalse();
        assertThat(response.likeCount()).isEqualTo(4);
        verify(threadLikeRepository).deleteByUserIdAndThreadId(USER_ID, THREAD_ID);
        verify(threadRepository).decrementLikeCount(THREAD_ID);
    }

    @Test
    void unlike_notLiked_isIdempotent() {
        threadWith(5, 0);
        when(threadLikeRepository.existsByUserIdAndThreadId(USER_ID, THREAD_ID)).thenReturn(false);

        ThreadLikeStatusResponse response = service.unlike(USER_ID, SLUG);

        assertThat(response.liked()).isFalse();
        assertThat(response.likeCount()).isEqualTo(5);
        verify(threadLikeRepository, never()).deleteByUserIdAndThreadId(USER_ID, THREAD_ID);
        verify(threadRepository, never()).decrementLikeCount(THREAD_ID);
    }

    @Test
    void bookmark_notAlreadyBookmarked_savesAndIncrementsCount() {
        threadWith(0, 2);
        when(threadBookmarkRepository.existsByUserIdAndThreadId(USER_ID, THREAD_ID)).thenReturn(false);

        ThreadBookmarkStatusResponse response = service.bookmark(USER_ID, SLUG);

        assertThat(response.bookmarked()).isTrue();
        assertThat(response.bookmarkCount()).isEqualTo(3);
        verify(threadBookmarkRepository).save(org.mockito.ArgumentMatchers.any());
        verify(threadRepository).incrementBookmarkCount(THREAD_ID);
    }

    @Test
    void bookmark_alreadyBookmarked_isIdempotent() {
        threadWith(0, 2);
        when(threadBookmarkRepository.existsByUserIdAndThreadId(USER_ID, THREAD_ID)).thenReturn(true);

        ThreadBookmarkStatusResponse response = service.bookmark(USER_ID, SLUG);

        assertThat(response.bookmarked()).isTrue();
        assertThat(response.bookmarkCount()).isEqualTo(2);
        verify(threadBookmarkRepository, never()).save(org.mockito.ArgumentMatchers.any());
        verify(threadRepository, never()).incrementBookmarkCount(THREAD_ID);
    }

    @Test
    void unbookmark_wasBookmarked_deletesAndDecrementsCount() {
        threadWith(0, 2);
        when(threadBookmarkRepository.existsByUserIdAndThreadId(USER_ID, THREAD_ID)).thenReturn(true);

        ThreadBookmarkStatusResponse response = service.unbookmark(USER_ID, SLUG);

        assertThat(response.bookmarked()).isFalse();
        assertThat(response.bookmarkCount()).isEqualTo(1);
        verify(threadBookmarkRepository).deleteByUserIdAndThreadId(USER_ID, THREAD_ID);
        verify(threadRepository).decrementBookmarkCount(THREAD_ID);
    }

    @Test
    void unbookmark_notBookmarked_isIdempotent() {
        threadWith(0, 2);
        when(threadBookmarkRepository.existsByUserIdAndThreadId(USER_ID, THREAD_ID)).thenReturn(false);

        ThreadBookmarkStatusResponse response = service.unbookmark(USER_ID, SLUG);

        assertThat(response.bookmarked()).isFalse();
        assertThat(response.bookmarkCount()).isEqualTo(2);
        verify(threadBookmarkRepository, never()).deleteByUserIdAndThreadId(USER_ID, THREAD_ID);
        verify(threadRepository, never()).decrementBookmarkCount(THREAD_ID);
    }
}
