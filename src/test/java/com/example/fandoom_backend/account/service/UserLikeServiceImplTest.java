package com.example.fandoom_backend.account.service;

import com.example.fandoom_backend.account.dto.LikeStatusResponse;
import com.example.fandoom_backend.account.dto.UserLikeResponse;
import com.example.fandoom_backend.account.entity.ActivityType;
import com.example.fandoom_backend.account.entity.SavedItemType;
import com.example.fandoom_backend.account.entity.UserLike;
import com.example.fandoom_backend.account.repository.UserLikeRepository;
import com.example.fandoom_backend.blog.service.BlogService;
import com.example.fandoom_backend.common.dto.PageResponse;
import com.example.fandoom_backend.common.exception.InvalidReferenceException;
import com.example.fandoom_backend.movie.service.MovieService;
import com.example.fandoom_backend.series.service.SeriesService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserLikeServiceImplTest {

    private static final Long USER_ID = 7L;
    private static final Long ITEM_ID = 1L;

    @Mock
    private UserLikeRepository userLikeRepository;
    @Mock
    private MovieService movieService;
    @Mock
    private SeriesService seriesService;
    @Mock
    private BlogService blogService;
    @Mock
    private ActivityLogService activityLogService;

    private UserLikeServiceImpl service;

    @BeforeEach
    void setUp() {
        ItemReferenceValidator validator = new ItemReferenceValidator(movieService, seriesService, blogService);
        service = new UserLikeServiceImpl(userLikeRepository, validator, activityLogService);
    }

    @Test
    void like_notYetLiked_validatesSavesAndLogsActivity() {
        when(userLikeRepository.findByUserIdAndItemTypeAndItemId(USER_ID, SavedItemType.MOVIE, ITEM_ID))
                .thenReturn(Optional.empty());
        when(movieService.existsById(ITEM_ID)).thenReturn(true);
        when(userLikeRepository.existsByUserIdAndItemTypeAndItemId(USER_ID, SavedItemType.MOVIE, ITEM_ID))
                .thenReturn(true);
        when(userLikeRepository.countByItemTypeAndItemId(SavedItemType.MOVIE, ITEM_ID)).thenReturn(1L);

        LikeStatusResponse response = service.like(USER_ID, SavedItemType.MOVIE, ITEM_ID);

        assertThat(response.liked()).isTrue();
        assertThat(response.likeCount()).isEqualTo(1L);
        verify(userLikeRepository).save(any(UserLike.class));
        verify(activityLogService).record(USER_ID, ActivityType.LIKED, ITEM_ID, SavedItemType.MOVIE);
    }

    @Test
    void like_alreadyLiked_idempotent_doesNotValidateOrSaveOrLogAgain() {
        UserLike existing = UserLike.builder().id(1L).userId(USER_ID).itemType(SavedItemType.MOVIE).itemId(ITEM_ID).build();
        when(userLikeRepository.findByUserIdAndItemTypeAndItemId(USER_ID, SavedItemType.MOVIE, ITEM_ID))
                .thenReturn(Optional.of(existing));
        when(userLikeRepository.existsByUserIdAndItemTypeAndItemId(USER_ID, SavedItemType.MOVIE, ITEM_ID))
                .thenReturn(true);
        when(userLikeRepository.countByItemTypeAndItemId(SavedItemType.MOVIE, ITEM_ID)).thenReturn(1L);

        LikeStatusResponse response = service.like(USER_ID, SavedItemType.MOVIE, ITEM_ID);

        assertThat(response.liked()).isTrue();
        verify(userLikeRepository, never()).save(any());
        verify(activityLogService, never()).record(any(), any(), any(), any());
        verify(movieService, never()).existsById(any());
    }

    @Test
    void like_invalidItem_throwsInvalidReferenceExceptionAndNeverSaves() {
        when(userLikeRepository.findByUserIdAndItemTypeAndItemId(USER_ID, SavedItemType.MOVIE, ITEM_ID))
                .thenReturn(Optional.empty());
        when(movieService.existsById(ITEM_ID)).thenReturn(false);

        assertThatThrownBy(() -> service.like(USER_ID, SavedItemType.MOVIE, ITEM_ID))
                .isInstanceOf(InvalidReferenceException.class);

        verify(userLikeRepository, never()).save(any());
    }

    @Test
    void unlike_currentlyLiked_deletesRow() {
        UserLike existing = UserLike.builder().id(1L).userId(USER_ID).itemType(SavedItemType.MOVIE).itemId(ITEM_ID).build();
        when(userLikeRepository.findByUserIdAndItemTypeAndItemId(USER_ID, SavedItemType.MOVIE, ITEM_ID))
                .thenReturn(Optional.of(existing));

        service.unlike(USER_ID, SavedItemType.MOVIE, ITEM_ID);

        verify(userLikeRepository).delete(existing);
    }

    @Test
    void unlike_notCurrentlyLiked_isNoOp() {
        when(userLikeRepository.findByUserIdAndItemTypeAndItemId(USER_ID, SavedItemType.MOVIE, ITEM_ID))
                .thenReturn(Optional.empty());

        service.unlike(USER_ID, SavedItemType.MOVIE, ITEM_ID);

        verify(userLikeRepository, never()).delete(any());
    }

    @Test
    void count_delegatesToRepository() {
        when(userLikeRepository.countByItemTypeAndItemId(SavedItemType.BLOG, 5L)).thenReturn(42L);

        assertThat(service.count(SavedItemType.BLOG, 5L)).isEqualTo(42L);
    }

    @Test
    void list_mapsPageOfLikesToResponses() {
        UserLike like = UserLike.builder().id(1L).userId(USER_ID).itemType(SavedItemType.BLOG).itemId(5L).build();
        like.setCreatedAt(LocalDateTime.of(2026, 1, 1, 0, 0));
        Pageable pageable = PageRequest.of(0, 20);
        when(userLikeRepository.findByUserId(USER_ID, pageable))
                .thenReturn(new PageImpl<>(List.of(like), pageable, 1));

        PageResponse<UserLikeResponse> result = service.list(USER_ID, pageable);

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).itemId()).isEqualTo(5L);
        assertThat(result.content().get(0).itemType()).isEqualTo(SavedItemType.BLOG);
    }
}
