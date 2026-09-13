package com.example.fandoom_backend.account.service;

import com.example.fandoom_backend.account.dto.FollowStatusResponse;
import com.example.fandoom_backend.account.dto.UserFollowResponse;
import com.example.fandoom_backend.account.entity.SavedItemType;
import com.example.fandoom_backend.account.entity.UserFollow;
import com.example.fandoom_backend.account.repository.UserFollowRepository;
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
class UserFollowServiceImplTest {

    private static final Long USER_ID = 7L;
    private static final Long ITEM_ID = 1L;

    @Mock
    private UserFollowRepository userFollowRepository;
    @Mock
    private MovieService movieService;
    @Mock
    private SeriesService seriesService;
    @Mock
    private BlogService blogService;
    @Mock
    private com.example.fandoom_backend.user.service.UserService userService;

    private UserFollowServiceImpl service;

    @BeforeEach
    void setUp() {
        ItemReferenceValidator validator = new ItemReferenceValidator(movieService, seriesService, blogService, userService);
        service = new UserFollowServiceImpl(userFollowRepository, validator);
    }

    @Test
    void follow_selfFollow_throwsInvalidReferenceExceptionBeforeValidatorOrRepository() {
        assertThatThrownBy(() -> service.follow(USER_ID, SavedItemType.USER, USER_ID))
                .isInstanceOf(InvalidReferenceException.class);

        verify(userFollowRepository, never()).findByUserIdAndItemTypeAndItemId(any(), any(), any());
        verify(userService, never()).existsById(any());
        verify(userFollowRepository, never()).save(any());
    }

    @Test
    void follow_userItemType_notSelf_validatesViaUserService() {
        Long otherUserId = 42L;
        when(userFollowRepository.findByUserIdAndItemTypeAndItemId(USER_ID, SavedItemType.USER, otherUserId))
                .thenReturn(Optional.empty());
        when(userService.existsById(otherUserId)).thenReturn(true);
        when(userFollowRepository.existsByUserIdAndItemTypeAndItemId(USER_ID, SavedItemType.USER, otherUserId))
                .thenReturn(true);
        when(userFollowRepository.countByItemTypeAndItemId(SavedItemType.USER, otherUserId)).thenReturn(1L);

        FollowStatusResponse response = service.follow(USER_ID, SavedItemType.USER, otherUserId);

        assertThat(response.following()).isTrue();
        verify(userService).existsById(otherUserId);
        verify(userFollowRepository).save(any(UserFollow.class));
    }

    @Test
    void countFollowing_delegatesToRepository() {
        when(userFollowRepository.countByUserIdAndItemType(USER_ID, SavedItemType.USER)).thenReturn(5L);

        assertThat(service.countFollowing(USER_ID, SavedItemType.USER)).isEqualTo(5L);
    }

    @Test
    void follow_notYetFollowing_validatesAndSaves() {
        when(userFollowRepository.findByUserIdAndItemTypeAndItemId(USER_ID, SavedItemType.SERIES, ITEM_ID))
                .thenReturn(Optional.empty());
        when(seriesService.existsById(ITEM_ID)).thenReturn(true);
        when(userFollowRepository.existsByUserIdAndItemTypeAndItemId(USER_ID, SavedItemType.SERIES, ITEM_ID))
                .thenReturn(true);
        when(userFollowRepository.countByItemTypeAndItemId(SavedItemType.SERIES, ITEM_ID)).thenReturn(1L);

        FollowStatusResponse response = service.follow(USER_ID, SavedItemType.SERIES, ITEM_ID);

        assertThat(response.following()).isTrue();
        assertThat(response.followerCount()).isEqualTo(1L);
        verify(userFollowRepository).save(any(UserFollow.class));
    }

    @Test
    void follow_alreadyFollowing_idempotent_doesNotSaveAgain() {
        UserFollow existing = UserFollow.builder().id(1L).userId(USER_ID).itemType(SavedItemType.SERIES).itemId(ITEM_ID).build();
        when(userFollowRepository.findByUserIdAndItemTypeAndItemId(USER_ID, SavedItemType.SERIES, ITEM_ID))
                .thenReturn(Optional.of(existing));
        when(userFollowRepository.existsByUserIdAndItemTypeAndItemId(USER_ID, SavedItemType.SERIES, ITEM_ID))
                .thenReturn(true);
        when(userFollowRepository.countByItemTypeAndItemId(SavedItemType.SERIES, ITEM_ID)).thenReturn(1L);

        service.follow(USER_ID, SavedItemType.SERIES, ITEM_ID);

        verify(userFollowRepository, never()).save(any());
        verify(seriesService, never()).existsById(any());
    }

    @Test
    void follow_invalidItem_throwsInvalidReferenceException() {
        when(userFollowRepository.findByUserIdAndItemTypeAndItemId(USER_ID, SavedItemType.SERIES, ITEM_ID))
                .thenReturn(Optional.empty());
        when(seriesService.existsById(ITEM_ID)).thenReturn(false);

        assertThatThrownBy(() -> service.follow(USER_ID, SavedItemType.SERIES, ITEM_ID))
                .isInstanceOf(InvalidReferenceException.class);

        verify(userFollowRepository, never()).save(any());
    }

    @Test
    void unfollow_currentlyFollowing_deletesRow() {
        UserFollow existing = UserFollow.builder().id(1L).userId(USER_ID).itemType(SavedItemType.SERIES).itemId(ITEM_ID).build();
        when(userFollowRepository.findByUserIdAndItemTypeAndItemId(USER_ID, SavedItemType.SERIES, ITEM_ID))
                .thenReturn(Optional.of(existing));

        service.unfollow(USER_ID, SavedItemType.SERIES, ITEM_ID);

        verify(userFollowRepository).delete(existing);
    }

    @Test
    void unfollow_notCurrentlyFollowing_isNoOp() {
        when(userFollowRepository.findByUserIdAndItemTypeAndItemId(USER_ID, SavedItemType.SERIES, ITEM_ID))
                .thenReturn(Optional.empty());

        service.unfollow(USER_ID, SavedItemType.SERIES, ITEM_ID);

        verify(userFollowRepository, never()).delete(any());
    }

    @Test
    void list_mapsPageOfFollowsToResponses() {
        UserFollow follow = UserFollow.builder().id(1L).userId(USER_ID).itemType(SavedItemType.MOVIE).itemId(5L).build();
        follow.setCreatedAt(LocalDateTime.of(2026, 1, 1, 0, 0));
        Pageable pageable = PageRequest.of(0, 20);
        when(userFollowRepository.findByUserId(USER_ID, pageable))
                .thenReturn(new PageImpl<>(List.of(follow), pageable, 1));

        PageResponse<UserFollowResponse> result = service.list(USER_ID, pageable);

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).itemId()).isEqualTo(5L);
        assertThat(result.content().get(0).itemType()).isEqualTo(SavedItemType.MOVIE);
    }

    @Test
    void count_delegatesToRepository() {
        when(userFollowRepository.countByItemTypeAndItemId(SavedItemType.MOVIE, 5L)).thenReturn(3L);

        assertThat(service.count(SavedItemType.MOVIE, 5L)).isEqualTo(3L);
    }

    @Test
    void getStatus_returnsCurrentFollowingStateAndCount() {
        when(userFollowRepository.existsByUserIdAndItemTypeAndItemId(USER_ID, SavedItemType.SERIES, ITEM_ID))
                .thenReturn(true);
        when(userFollowRepository.countByItemTypeAndItemId(SavedItemType.SERIES, ITEM_ID)).thenReturn(4L);

        FollowStatusResponse response = service.getStatus(USER_ID, SavedItemType.SERIES, ITEM_ID);

        assertThat(response.following()).isTrue();
        assertThat(response.followerCount()).isEqualTo(4L);
    }
}
