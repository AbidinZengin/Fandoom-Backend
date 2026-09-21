package com.example.fandoom_backend.community.service;

import com.example.fandoom_backend.common.exception.ResourceNotFoundException;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Id varyantları: slug varyantlarıyla AYNI davranış, ama thread id ile bulunur. Yok/DELETED/HIDDEN thread ve portalı
// HIDDEN olan thread findVisibleById'da boş döner -> 404 ve hiçbir yazma/sayaç işlemi yapılmaz.
@ExtendWith(MockitoExtension.class)
class ThreadInteractionByIdTest {

    private static final Long USER_ID = 1L;
    private static final Long THREAD_ID = 10L;

    @Mock private ThreadRepository threadRepository;
    @Mock private ThreadLikeRepository threadLikeRepository;
    @Mock private ThreadBookmarkRepository threadBookmarkRepository;

    private ThreadInteractionServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ThreadInteractionServiceImpl(threadRepository, threadLikeRepository, threadBookmarkRepository);
    }

    private void visibleThread(int likes, int bookmarks) {
        when(threadRepository.findVisibleById(THREAD_ID)).thenReturn(Optional.of(
                Thread.builder().id(THREAD_ID).status(ThreadStatus.PUBLISHED).likeCount(likes)
                        .bookmarkCount(bookmarks).build()));
    }

    @Test
    void likeById_newLike_incrementsAndReturnsCountPlusOne_andTwiceIsIdempotent() {
        visibleThread(5, 0);
        when(threadLikeRepository.existsByUserIdAndThreadId(USER_ID, THREAD_ID)).thenReturn(false, true);

        ThreadLikeStatusResponse first = service.like(USER_ID, THREAD_ID);
        ThreadLikeStatusResponse second = service.like(USER_ID, THREAD_ID);

        assertThat(first.likeCount()).isEqualTo(6);
        assertThat(second.likeCount()).isEqualTo(5);
        verify(threadRepository).incrementLikeCount(THREAD_ID); // yalnız bir kez
    }

    @Test
    void unlikeById_existingLike_decrements() {
        visibleThread(5, 0);
        when(threadLikeRepository.existsByUserIdAndThreadId(USER_ID, THREAD_ID)).thenReturn(true);

        ThreadLikeStatusResponse response = service.unlike(USER_ID, THREAD_ID);

        assertThat(response.liked()).isFalse();
        assertThat(response.likeCount()).isEqualTo(4);
        verify(threadRepository).decrementLikeCount(THREAD_ID);
    }

    @Test
    void bookmarkByIdAndUnbookmarkById_workLikeSlugVariants() {
        visibleThread(0, 2);
        when(threadBookmarkRepository.existsByUserIdAndThreadId(USER_ID, THREAD_ID)).thenReturn(false, true);

        ThreadBookmarkStatusResponse added = service.bookmark(USER_ID, THREAD_ID);
        ThreadBookmarkStatusResponse removed = service.unbookmark(USER_ID, THREAD_ID);

        assertThat(added.bookmarkCount()).isEqualTo(3);
        assertThat(removed.bookmarked()).isFalse();
        assertThat(removed.bookmarkCount()).isEqualTo(1);
    }

    @Test
    void invisibleThreadById_is404_andNothingIsWritten() {
        when(threadRepository.findVisibleById(THREAD_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.like(USER_ID, THREAD_ID)).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.unlike(USER_ID, THREAD_ID)).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.bookmark(USER_ID, THREAD_ID)).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.unbookmark(USER_ID, THREAD_ID)).isInstanceOf(ResourceNotFoundException.class);

        verify(threadLikeRepository, never()).save(any());
        verify(threadBookmarkRepository, never()).save(any());
        verify(threadRepository, never()).incrementLikeCount(any());
    }

    @Test
    @SuppressWarnings("deprecation")
    void invisibleThreadBySlug_isAlso404() {
        when(threadRepository.findVisibleBySlug("gizli-portal-thread")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.like(USER_ID, "gizli-portal-thread"))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
