package com.example.fandoom_backend.community.service;

import com.example.fandoom_backend.community.dto.TagFollowStatusResponse;
import com.example.fandoom_backend.community.entity.ThreadStatus;
import com.example.fandoom_backend.community.repository.TagFollowRepository;
import com.example.fandoom_backend.community.repository.ThreadTagRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// ThreadLike/ThreadBookmark ile aynı idempotent toggle deseni (bkz.
// ThreadInteractionServiceImplTest).
@ExtendWith(MockitoExtension.class)
class TagFollowServiceImplTest {

    private static final Long USER_ID = 1L;

    @Mock private TagFollowRepository tagFollowRepository;
    @Mock private ThreadTagRepository threadTagRepository;

    private TagFollowServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new TagFollowServiceImpl(tagFollowRepository, threadTagRepository);
    }

    @Test
    void follow_notAlreadyFollowing_saves() {
        when(tagFollowRepository.existsByUserIdAndTag(USER_ID, "teori")).thenReturn(false);

        TagFollowStatusResponse response = service.follow(USER_ID, "Teori");

        assertThat(response.following()).isTrue();
        verify(tagFollowRepository).save(any());
    }

    @Test
    void follow_alreadyFollowing_isIdempotent() {
        when(tagFollowRepository.existsByUserIdAndTag(USER_ID, "teori")).thenReturn(true);

        TagFollowStatusResponse response = service.follow(USER_ID, "Teori");

        assertThat(response.following()).isTrue();
        verify(tagFollowRepository, never()).save(any());
    }

    @Test
    void unfollow_alwaysDeletes_andReturnsFollowingFalse() {
        TagFollowStatusResponse response = service.unfollow(USER_ID, "Teori");

        assertThat(response.following()).isFalse();
        verify(tagFollowRepository).deleteByUserIdAndTag(USER_ID, "teori");
    }

    @Test
    void getTrending_windowNull_defaultsTo7DaysAgo() {
        ArgumentCaptor<LocalDateTime> sinceCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        when(threadTagRepository.findTrending(eq(ThreadStatus.PUBLISHED), sinceCaptor.capture(), any(Pageable.class)))
                .thenReturn(List.of());

        service.getTrending(null, 10);

        LocalDateTime expected = LocalDateTime.now().minusDays(7);
        assertThat(sinceCaptor.getValue()).isCloseTo(expected, within(5, ChronoUnit.SECONDS));
    }

    @Test
    void getTrending_window7d_resolvesTo7DaysAgo() {
        ArgumentCaptor<LocalDateTime> sinceCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        when(threadTagRepository.findTrending(eq(ThreadStatus.PUBLISHED), sinceCaptor.capture(), any(Pageable.class)))
                .thenReturn(List.of());

        service.getTrending("7d", 10);

        LocalDateTime expected = LocalDateTime.now().minusDays(7);
        assertThat(sinceCaptor.getValue()).isCloseTo(expected, within(5, ChronoUnit.SECONDS));
    }

    @Test
    void getTrending_window30d_resolvesTo30DaysAgo() {
        ArgumentCaptor<LocalDateTime> sinceCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        when(threadTagRepository.findTrending(eq(ThreadStatus.PUBLISHED), sinceCaptor.capture(), any(Pageable.class)))
                .thenReturn(List.of());

        service.getTrending("30d", 10);

        LocalDateTime expected = LocalDateTime.now().minusDays(30);
        assertThat(sinceCaptor.getValue()).isCloseTo(expected, within(5, ChronoUnit.SECONDS));
    }
}
