package com.example.fandoom_backend.account.service;

import com.example.fandoom_backend.account.dto.SaveItemRequest;
import com.example.fandoom_backend.account.dto.SavedItemStatusResponse;
import com.example.fandoom_backend.account.dto.UpdateSavedItemRequest;
import com.example.fandoom_backend.account.dto.UserSavedItemResponse;
import com.example.fandoom_backend.account.entity.ActivityType;
import com.example.fandoom_backend.account.entity.ListType;
import com.example.fandoom_backend.account.entity.SavedItemType;
import com.example.fandoom_backend.account.entity.UserList;
import com.example.fandoom_backend.account.entity.UserSavedItem;
import com.example.fandoom_backend.account.mapper.UserSavedItemMapper;
import com.example.fandoom_backend.account.repository.UserListRepository;
import com.example.fandoom_backend.account.repository.UserSavedItemRepository;
import com.example.fandoom_backend.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserSavedItemServiceImpl implements UserSavedItemService {

    private final UserSavedItemRepository userSavedItemRepository;
    private final UserListRepository userListRepository;
    private final UserSavedItemMapper userSavedItemMapper;
    private final ItemReferenceValidator itemReferenceValidator;
    private final SystemListRegistry systemListRegistry;
    private final ActivityLogService activityLogService;

    @Override
    @Transactional
    public UserSavedItemResponse save(Long userId, SaveItemRequest request) {
        itemReferenceValidator.assertExists(request.itemType(), request.itemId());
        UserList targetList = request.targetListId() != null
                ? findOwnedList(userId, request.targetListId())
                : systemListRegistry.resolve(userId, defaultListType(request.itemType()));

        UserSavedItem item = UserSavedItem.builder()
                .userId(userId)
                .itemId(request.itemId())
                .itemType(request.itemType())
                .userList(targetList)
                .notes(request.notes())
                .build();
        item = userSavedItemRepository.save(item);

        if (targetList.getListType() == ListType.WATCHLIST) {
            activityLogService.record(userId, ActivityType.ADDED_TO_WATCHLIST, request.itemId(), request.itemType());
        }
        return userSavedItemMapper.toResponse(item);
    }

    @Override
    @Transactional
    public UserSavedItemResponse update(Long userId, Long id, UpdateSavedItemRequest request) {
        UserSavedItem item = findOwned(userId, id);
        if (request.progressPercentage() != null) {
            item.setProgressPercentage(request.progressPercentage());
        }
        if (request.notes() != null) {
            item.setNotes(request.notes());
        }
        return userSavedItemMapper.toResponse(item);
    }

    @Override
    @Transactional
    public void delete(Long userId, Long id) {
        UserSavedItem item = findOwned(userId, id);
        userSavedItemRepository.delete(item);
    }

    @Override
    public SavedItemStatusResponse getStatus(Long userId, SavedItemType itemType, Long itemId) {
        return userSavedItemRepository
                .findByUserIdAndItemTypeAndItemIdAndUserList_ListType(userId, itemType, itemId, defaultListType(itemType))
                .map(item -> new SavedItemStatusResponse(true, item.getId()))
                .orElseGet(() -> new SavedItemStatusResponse(false, null));
    }

    private ListType defaultListType(SavedItemType itemType) {
        return itemType == SavedItemType.BLOG ? ListType.READLIST : ListType.WATCHLIST;
    }

    private UserList findOwnedList(Long userId, Long listId) {
        return userListRepository.findByIdAndUserId(listId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Liste bulunamadı: id=" + listId));
    }

    private UserSavedItem findOwned(Long userId, Long id) {
        return userSavedItemRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Kaydedilen öğe bulunamadı: id=" + id));
    }
}
