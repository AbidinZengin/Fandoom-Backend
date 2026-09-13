package com.example.fandoom_backend.account.service;

import com.example.fandoom_backend.account.dto.BookmarkStatusResponse;
import com.example.fandoom_backend.account.dto.UserBookmarkResponse;
import com.example.fandoom_backend.account.entity.ActivityType;
import com.example.fandoom_backend.account.entity.SavedItemType;
import com.example.fandoom_backend.account.entity.UserBookmark;
import com.example.fandoom_backend.account.repository.UserBookmarkRepository;
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
class UserBookmarkServiceImplTest {

    private static final Long USER_ID = 7L;
    private static final Long ITEM_ID = 1L;

    @Mock
    private UserBookmarkRepository userBookmarkRepository;
    @Mock
    private MovieService movieService;
    @Mock
    private SeriesService seriesService;
    @Mock
    private BlogService blogService;
    @Mock
    private com.example.fandoom_backend.user.service.UserService userService;
    @Mock
    private ActivityLogService activityLogService;

    private UserBookmarkServiceImpl service;

    @BeforeEach
    void setUp() {
        ItemReferenceValidator validator = new ItemReferenceValidator(movieService, seriesService, blogService, userService);
        service = new UserBookmarkServiceImpl(userBookmarkRepository, validator, activityLogService);
    }

    @Test
    void bookmark_notYetBookmarked_validatesSavesAndLogsActivity() {
        when(userBookmarkRepository.findByUserIdAndItemTypeAndItemId(USER_ID, SavedItemType.MOVIE, ITEM_ID))
                .thenReturn(Optional.empty());
        when(movieService.existsById(ITEM_ID)).thenReturn(true);
        when(userBookmarkRepository.existsByUserIdAndItemTypeAndItemId(USER_ID, SavedItemType.MOVIE, ITEM_ID))
                .thenReturn(true);
        when(userBookmarkRepository.countByItemTypeAndItemId(SavedItemType.MOVIE, ITEM_ID)).thenReturn(1L);

        BookmarkStatusResponse response = service.bookmark(USER_ID, SavedItemType.MOVIE, ITEM_ID);

        assertThat(response.bookmarked()).isTrue();
        assertThat(response.bookmarkCount()).isEqualTo(1L);
        verify(userBookmarkRepository).save(any(UserBookmark.class));
        verify(activityLogService).record(USER_ID, ActivityType.BOOKMARKED, ITEM_ID, SavedItemType.MOVIE);
    }

    @Test
    void bookmark_alreadyBookmarked_idempotent_doesNotValidateOrSaveOrLogAgain() {
        UserBookmark existing = UserBookmark.builder().id(1L).userId(USER_ID).itemType(SavedItemType.MOVIE).itemId(ITEM_ID).build();
        when(userBookmarkRepository.findByUserIdAndItemTypeAndItemId(USER_ID, SavedItemType.MOVIE, ITEM_ID))
                .thenReturn(Optional.of(existing));
        when(userBookmarkRepository.existsByUserIdAndItemTypeAndItemId(USER_ID, SavedItemType.MOVIE, ITEM_ID))
                .thenReturn(true);
        when(userBookmarkRepository.countByItemTypeAndItemId(SavedItemType.MOVIE, ITEM_ID)).thenReturn(1L);

        BookmarkStatusResponse response = service.bookmark(USER_ID, SavedItemType.MOVIE, ITEM_ID);

        assertThat(response.bookmarked()).isTrue();
        verify(userBookmarkRepository, never()).save(any());
        verify(activityLogService, never()).record(any(), any(), any(), any());
        verify(movieService, never()).existsById(any());
    }

    @Test
    void bookmark_invalidItem_throwsInvalidReferenceExceptionAndNeverSaves() {
        when(userBookmarkRepository.findByUserIdAndItemTypeAndItemId(USER_ID, SavedItemType.MOVIE, ITEM_ID))
                .thenReturn(Optional.empty());
        when(movieService.existsById(ITEM_ID)).thenReturn(false);

        assertThatThrownBy(() -> service.bookmark(USER_ID, SavedItemType.MOVIE, ITEM_ID))
                .isInstanceOf(InvalidReferenceException.class);

        verify(userBookmarkRepository, never()).save(any());
    }

    @Test
    void unbookmark_currentlyBookmarked_deletesRow() {
        UserBookmark existing = UserBookmark.builder().id(1L).userId(USER_ID).itemType(SavedItemType.MOVIE).itemId(ITEM_ID).build();
        when(userBookmarkRepository.findByUserIdAndItemTypeAndItemId(USER_ID, SavedItemType.MOVIE, ITEM_ID))
                .thenReturn(Optional.of(existing));

        service.unbookmark(USER_ID, SavedItemType.MOVIE, ITEM_ID);

        verify(userBookmarkRepository).delete(existing);
    }

    @Test
    void unbookmark_notCurrentlyBookmarked_isNoOp() {
        when(userBookmarkRepository.findByUserIdAndItemTypeAndItemId(USER_ID, SavedItemType.MOVIE, ITEM_ID))
                .thenReturn(Optional.empty());

        service.unbookmark(USER_ID, SavedItemType.MOVIE, ITEM_ID);

        verify(userBookmarkRepository, never()).delete(any());
    }

    @Test
    void count_delegatesToRepository() {
        when(userBookmarkRepository.countByItemTypeAndItemId(SavedItemType.BLOG, 5L)).thenReturn(42L);

        assertThat(service.count(SavedItemType.BLOG, 5L)).isEqualTo(42L);
    }

    @Test
    void list_mapsPageOfBookmarksToResponses() {
        UserBookmark bookmark = UserBookmark.builder().id(1L).userId(USER_ID).itemType(SavedItemType.BLOG).itemId(5L).build();
        bookmark.setCreatedAt(LocalDateTime.of(2026, 1, 1, 0, 0));
        Pageable pageable = PageRequest.of(0, 20);
        when(userBookmarkRepository.findByUserId(USER_ID, pageable))
                .thenReturn(new PageImpl<>(List.of(bookmark), pageable, 1));

        PageResponse<UserBookmarkResponse> result = service.list(USER_ID, pageable);

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).itemId()).isEqualTo(5L);
        assertThat(result.content().get(0).itemType()).isEqualTo(SavedItemType.BLOG);
    }
}
