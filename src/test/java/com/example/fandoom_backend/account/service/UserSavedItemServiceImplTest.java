package com.example.fandoom_backend.account.service;

import com.example.fandoom_backend.account.dto.SaveItemRequest;
import com.example.fandoom_backend.account.dto.SavedItemStatusResponse;
import com.example.fandoom_backend.account.dto.UpdateSavedItemRequest;
import com.example.fandoom_backend.account.entity.ActivityType;
import com.example.fandoom_backend.account.entity.ListType;
import com.example.fandoom_backend.account.entity.SavedItemType;
import com.example.fandoom_backend.account.entity.UserList;
import com.example.fandoom_backend.account.entity.UserSavedItem;
import com.example.fandoom_backend.account.mapper.UserSavedItemMapper;
import com.example.fandoom_backend.account.repository.UserListRepository;
import com.example.fandoom_backend.account.repository.UserSavedItemRepository;
import com.example.fandoom_backend.blog.service.BlogService;
import com.example.fandoom_backend.common.exception.InvalidReferenceException;
import com.example.fandoom_backend.common.exception.ResourceNotFoundException;
import com.example.fandoom_backend.movie.service.MovieService;
import com.example.fandoom_backend.series.service.SeriesService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserSavedItemServiceImplTest {

    private static final Long USER_ID = 7L;

    @Mock
    private UserSavedItemRepository userSavedItemRepository;
    @Mock
    private UserListRepository userListRepository;
    @Mock
    private UserSavedItemMapper userSavedItemMapper;
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

    private UserSavedItemServiceImpl service;

    @BeforeEach
    void setUp() {
        ItemReferenceValidator validator = new ItemReferenceValidator(movieService, seriesService, blogService, userService);
        SystemListRegistry systemListRegistry = new SystemListRegistry(userListRepository);
        service = new UserSavedItemServiceImpl(userSavedItemRepository, userListRepository, userSavedItemMapper,
                validator, systemListRegistry, activityLogService);

        lenient().when(userSavedItemRepository.save(any(UserSavedItem.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void save_invalidMovieId_throwsInvalidReferenceExceptionAndNeverSaves() {
        when(movieService.existsById(404L)).thenReturn(false);
        SaveItemRequest request = new SaveItemRequest(404L, SavedItemType.MOVIE, null, null, null);

        assertThatThrownBy(() -> service.save(USER_ID, request))
                .isInstanceOf(InvalidReferenceException.class);

        verify(userSavedItemRepository, never()).save(any());
    }

    @Test
    void save_validMovieWithoutTargetList_resolvesSystemWatchlistAndLogsActivity() {
        when(movieService.existsById(1L)).thenReturn(true);
        when(userListRepository.findByUserIdAndListType(USER_ID, ListType.WATCHLIST)).thenReturn(Optional.empty());
        when(userListRepository.save(any(UserList.class))).thenAnswer(inv -> inv.getArgument(0));
        SaveItemRequest request = new SaveItemRequest(1L, SavedItemType.MOVIE, null, null, null);

        service.save(USER_ID, request);

        ArgumentCaptor<UserSavedItem> captor = ArgumentCaptor.forClass(UserSavedItem.class);
        verify(userSavedItemRepository).save(captor.capture());
        assertThat(captor.getValue().getUserList().getListType()).isEqualTo(ListType.WATCHLIST);
        verify(activityLogService).record(USER_ID, ActivityType.ADDED_TO_WATCHLIST, 1L, SavedItemType.MOVIE);
    }

    @Test
    void save_validBlogWithoutTargetList_resolvesSystemReadlistAndDoesNotLogWatchlistActivity() {
        when(blogService.existsById(2L)).thenReturn(true);
        when(userListRepository.findByUserIdAndListType(USER_ID, ListType.READLIST)).thenReturn(Optional.empty());
        when(userListRepository.save(any(UserList.class))).thenAnswer(inv -> inv.getArgument(0));
        SaveItemRequest request = new SaveItemRequest(2L, SavedItemType.BLOG, null, null, null);

        service.save(USER_ID, request);

        verify(activityLogService, never()).record(any(), any(), any(), any());
    }

    @Test
    void save_withTargetListIdOwned_usesProvidedListInsteadOfSystemList() {
        when(movieService.existsById(1L)).thenReturn(true);
        UserList custom = UserList.builder().id(9L).userId(USER_ID).listType(ListType.CUSTOM).build();
        when(userListRepository.findByIdAndUserId(9L, USER_ID)).thenReturn(Optional.of(custom));
        SaveItemRequest request = new SaveItemRequest(1L, SavedItemType.MOVIE, 9L, null, null);

        service.save(USER_ID, request);

        verify(userListRepository, never()).findByUserIdAndListType(any(), any());
        verify(activityLogService, never()).record(any(), any(), any(), any());
    }

    @Test
    void save_withTargetListIdNotOwned_throwsResourceNotFoundException() {
        when(movieService.existsById(1L)).thenReturn(true);
        when(userListRepository.findByIdAndUserId(9L, USER_ID)).thenReturn(Optional.empty());
        SaveItemRequest request = new SaveItemRequest(1L, SavedItemType.MOVIE, 9L, null, null);

        assertThatThrownBy(() -> service.save(USER_ID, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void update_notOwned_throwsResourceNotFoundException() {
        when(userSavedItemRepository.findByIdAndUserId(1L, USER_ID)).thenReturn(Optional.empty());
        UpdateSavedItemRequest request = new UpdateSavedItemRequest(50, null);

        assertThatThrownBy(() -> service.update(USER_ID, 1L, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void update_partialFields_onlyUpdatesProvidedFields() {
        UserSavedItem existing = UserSavedItem.builder().id(1L).userId(USER_ID)
                .progressPercentage(10).notes("eski not").build();
        when(userSavedItemRepository.findByIdAndUserId(1L, USER_ID)).thenReturn(Optional.of(existing));
        UpdateSavedItemRequest request = new UpdateSavedItemRequest(75, null);

        service.update(USER_ID, 1L, request);

        assertThat(existing.getProgressPercentage()).isEqualTo(75);
        assertThat(existing.getNotes()).isEqualTo("eski not");
    }

    @Test
    void delete_notOwned_throwsResourceNotFoundException() {
        when(userSavedItemRepository.findByIdAndUserId(1L, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(USER_ID, 1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void delete_owned_deletesItem() {
        UserSavedItem existing = UserSavedItem.builder().id(1L).userId(USER_ID).build();
        when(userSavedItemRepository.findByIdAndUserId(1L, USER_ID)).thenReturn(Optional.of(existing));

        service.delete(USER_ID, 1L);

        verify(userSavedItemRepository).delete(existing);
    }

    @Test
    void getStatus_savedInDefaultSystemList_returnsSavedTrueWithId() {
        UserSavedItem existing = UserSavedItem.builder().id(5L).userId(USER_ID)
                .itemType(SavedItemType.MOVIE).itemId(1L).build();
        when(userSavedItemRepository.findByUserIdAndItemTypeAndItemIdAndUserList_ListType(
                USER_ID, SavedItemType.MOVIE, 1L, ListType.WATCHLIST)).thenReturn(Optional.of(existing));

        SavedItemStatusResponse response = service.getStatus(USER_ID, SavedItemType.MOVIE, 1L, null);

        assertThat(response.saved()).isTrue();
        assertThat(response.savedItemId()).isEqualTo(5L);
    }

    @Test
    void getStatus_notSaved_returnsSavedFalseWithNullId() {
        when(userSavedItemRepository.findByUserIdAndItemTypeAndItemIdAndUserList_ListType(
                USER_ID, SavedItemType.BLOG, 2L, ListType.READLIST)).thenReturn(Optional.empty());

        SavedItemStatusResponse response = service.getStatus(USER_ID, SavedItemType.BLOG, 2L, null);

        assertThat(response.saved()).isFalse();
        assertThat(response.savedItemId()).isNull();
    }

    @Test
    void getStatus_explicitWatchedListType_checksWatchedNotWatchlist() {
        UserSavedItem existing = UserSavedItem.builder().id(6L).userId(USER_ID)
                .itemType(SavedItemType.MOVIE).itemId(1L).build();
        when(userSavedItemRepository.findByUserIdAndItemTypeAndItemIdAndUserList_ListType(
                USER_ID, SavedItemType.MOVIE, 1L, ListType.WATCHED)).thenReturn(Optional.of(existing));

        SavedItemStatusResponse response = service.getStatus(USER_ID, SavedItemType.MOVIE, 1L, ListType.WATCHED);

        assertThat(response.saved()).isTrue();
        assertThat(response.savedItemId()).isEqualTo(6L);
    }

    @Test
    void save_withListTypeCustom_throwsInvalidReferenceException() {
        SaveItemRequest request = new SaveItemRequest(1L, SavedItemType.MOVIE, null, ListType.CUSTOM, null);

        assertThatThrownBy(() -> service.save(USER_ID, request))
                .isInstanceOf(InvalidReferenceException.class);

        verify(userSavedItemRepository, never()).save(any());
    }

    @Test
    void save_withListTypeWatched_removesExistingWatchlistEntryAndLogsMarkedWatched() {
        when(movieService.existsById(1L)).thenReturn(true);
        when(userListRepository.findByUserIdAndListType(USER_ID, ListType.WATCHED)).thenReturn(Optional.empty());
        when(userListRepository.save(any(UserList.class))).thenAnswer(inv -> inv.getArgument(0));
        UserSavedItem existingWatchlistEntry = UserSavedItem.builder().id(3L).userId(USER_ID)
                .itemType(SavedItemType.MOVIE).itemId(1L).build();
        when(userSavedItemRepository.findByUserIdAndItemTypeAndItemIdAndUserList_ListType(
                USER_ID, SavedItemType.MOVIE, 1L, ListType.WATCHLIST)).thenReturn(Optional.of(existingWatchlistEntry));
        SaveItemRequest request = new SaveItemRequest(1L, SavedItemType.MOVIE, null, ListType.WATCHED, null);

        service.save(USER_ID, request);

        verify(userSavedItemRepository).delete(existingWatchlistEntry);
        verify(activityLogService).record(USER_ID, ActivityType.MARKED_WATCHED, 1L, SavedItemType.MOVIE);
        verify(activityLogService, never()).record(USER_ID, ActivityType.ADDED_TO_WATCHLIST, 1L, SavedItemType.MOVIE);
    }
}
