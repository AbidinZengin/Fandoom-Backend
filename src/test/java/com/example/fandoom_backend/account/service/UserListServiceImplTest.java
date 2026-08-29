package com.example.fandoom_backend.account.service;

import com.example.fandoom_backend.account.dto.CreateUserListRequest;
import com.example.fandoom_backend.account.dto.UserListSummaryResponse;
import com.example.fandoom_backend.account.entity.ListType;
import com.example.fandoom_backend.account.entity.UserList;
import com.example.fandoom_backend.account.mapper.UserListMapper;
import com.example.fandoom_backend.account.mapper.UserSavedItemMapper;
import com.example.fandoom_backend.account.repository.UserListRepository;
import com.example.fandoom_backend.account.repository.UserSavedItemRepository;
import com.example.fandoom_backend.common.exception.InvalidReferenceException;
import com.example.fandoom_backend.common.exception.ResourceNotFoundException;
import com.example.fandoom_backend.media.service.ImageStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserListServiceImplTest {

    private static final Long USER_ID = 7L;

    @Mock
    private UserListRepository userListRepository;
    @Mock
    private UserSavedItemRepository userSavedItemRepository;
    @Mock
    private UserListMapper userListMapper;
    @Mock
    private UserSavedItemMapper userSavedItemMapper;
    @Mock
    private ImageStorageService imageStorageService;

    private UserListServiceImpl service;

    @BeforeEach
    void setUp() {
        SystemListRegistry systemListRegistry = new SystemListRegistry(userListRepository);
        service = new UserListServiceImpl(userListRepository, userSavedItemRepository, userListMapper,
                userSavedItemMapper, imageStorageService, systemListRegistry, new PartialUpdateValidator());

        lenient().when(userListRepository.save(any(UserList.class))).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(userListMapper.toSummaryResponse(any(), anyLong()))
                .thenReturn(new UserListSummaryResponse(1L, "t", null, ListType.CUSTOM, 0, false, false));
    }

    @Test
    void list_systemListsMissing_lazyCreatesWatchlistAndReadlist() {
        when(userListRepository.findByUserIdAndListType(USER_ID, ListType.WATCHLIST)).thenReturn(Optional.empty());
        when(userListRepository.findByUserIdAndListType(USER_ID, ListType.READLIST)).thenReturn(Optional.empty());
        when(userListRepository.findByUserIdOrderByIdAsc(USER_ID)).thenReturn(List.of());

        service.list(USER_ID);

        ArgumentCaptor<UserList> captor = ArgumentCaptor.forClass(UserList.class);
        verify(userListRepository, times(2)).save(captor.capture());
        List<ListType> savedTypes = captor.getAllValues().stream().map(UserList::getListType).toList();
        assertThat(savedTypes).containsExactlyInAnyOrder(ListType.WATCHLIST, ListType.READLIST);
    }

    @Test
    void list_systemListsAlreadyExist_doesNotRecreate() {
        UserList watchlist = UserList.builder().id(1L).userId(USER_ID).listType(ListType.WATCHLIST).build();
        UserList readlist = UserList.builder().id(2L).userId(USER_ID).listType(ListType.READLIST).build();
        when(userListRepository.findByUserIdAndListType(USER_ID, ListType.WATCHLIST)).thenReturn(Optional.of(watchlist));
        when(userListRepository.findByUserIdAndListType(USER_ID, ListType.READLIST)).thenReturn(Optional.of(readlist));
        when(userListRepository.findByUserIdOrderByIdAsc(USER_ID)).thenReturn(List.of(watchlist, readlist));

        service.list(USER_ID);

        verify(userListRepository, never()).save(any());
    }

    @Test
    void delete_systemList_throwsInvalidReferenceExceptionAndDoesNotDelete() {
        UserList watchlist = UserList.builder().id(1L).userId(USER_ID).listType(ListType.WATCHLIST).build();
        when(userListRepository.findByIdAndUserId(1L, USER_ID)).thenReturn(Optional.of(watchlist));

        assertThatThrownBy(() -> service.delete(USER_ID, 1L))
                .isInstanceOf(InvalidReferenceException.class);

        verify(userListRepository, never()).delete(any());
        verify(userSavedItemRepository, never()).deleteByUserListId(any());
    }

    @Test
    void delete_customList_deletesItsSavedItemsThenTheList() {
        UserList custom = UserList.builder().id(3L).userId(USER_ID).listType(ListType.CUSTOM)
                .coverImageUrl("https://cover.png").build();
        when(userListRepository.findByIdAndUserId(3L, USER_ID)).thenReturn(Optional.of(custom));

        service.delete(USER_ID, 3L);

        verify(imageStorageService).delete("https://cover.png");
        verify(userSavedItemRepository).deleteByUserListId(3L);
        verify(userListRepository).delete(custom);
    }

    @Test
    void delete_notOwned_throwsResourceNotFoundException() {
        when(userListRepository.findByIdAndUserId(99L, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(USER_ID, 99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void togglePin_belowMax_pinsSuccessfully() {
        UserList list = UserList.builder().id(1L).userId(USER_ID).listType(ListType.CUSTOM).isPinned(false).build();
        when(userListRepository.findByIdAndUserId(1L, USER_ID)).thenReturn(Optional.of(list));
        when(userListRepository.countByUserIdAndIsPinnedTrue(USER_ID)).thenReturn(5L);

        service.togglePin(USER_ID, 1L);

        assertThat(list.isPinned()).isTrue();
    }

    @Test
    void togglePin_alreadyAtMax_throwsInvalidReferenceException() {
        UserList list = UserList.builder().id(1L).userId(USER_ID).listType(ListType.CUSTOM).isPinned(false).build();
        when(userListRepository.findByIdAndUserId(1L, USER_ID)).thenReturn(Optional.of(list));
        when(userListRepository.countByUserIdAndIsPinnedTrue(USER_ID)).thenReturn(6L);

        assertThatThrownBy(() -> service.togglePin(USER_ID, 1L))
                .isInstanceOf(InvalidReferenceException.class);
        assertThat(list.isPinned()).isFalse();
    }

    @Test
    void togglePin_unpinning_neverBlockedByMaxLimit() {
        UserList list = UserList.builder().id(1L).userId(USER_ID).listType(ListType.CUSTOM).isPinned(true).build();
        when(userListRepository.findByIdAndUserId(1L, USER_ID)).thenReturn(Optional.of(list));

        service.togglePin(USER_ID, 1L);

        assertThat(list.isPinned()).isFalse();
        verify(userListRepository, never()).countByUserIdAndIsPinnedTrue(any());
    }

    @Test
    void getById_notOwned_throwsResourceNotFoundException() {
        when(userListRepository.findByIdAndUserId(1L, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(USER_ID, 1L, Pageable.unpaged()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void create_alwaysUsesCustomListTypeRegardlessOfInput() {
        CreateUserListRequest request = new CreateUserListRequest("My List", "desc", null, true);
        when(userSavedItemRepository.findByUserListId(any(), any())).thenReturn(Page.empty());

        service.create(USER_ID, request);

        ArgumentCaptor<UserList> captor = ArgumentCaptor.forClass(UserList.class);
        verify(userListRepository).save(captor.capture());
        assertThat(captor.getValue().getListType()).isEqualTo(ListType.CUSTOM);
        assertThat(captor.getValue().isPublic()).isTrue();
    }
}
