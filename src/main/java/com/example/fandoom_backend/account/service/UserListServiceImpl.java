package com.example.fandoom_backend.account.service;

import com.example.fandoom_backend.account.dto.CreateUserListRequest;
import com.example.fandoom_backend.account.dto.UserListDetailResponse;
import com.example.fandoom_backend.account.dto.UserListSummaryResponse;
import com.example.fandoom_backend.account.dto.UserSavedItemResponse;
import com.example.fandoom_backend.account.entity.ListType;
import com.example.fandoom_backend.account.entity.UserList;
import com.example.fandoom_backend.account.mapper.UserListMapper;
import com.example.fandoom_backend.account.mapper.UserSavedItemMapper;
import com.example.fandoom_backend.account.repository.UserListRepository;
import com.example.fandoom_backend.account.repository.UserSavedItemRepository;
import com.example.fandoom_backend.common.dto.PageResponse;
import com.example.fandoom_backend.common.exception.InvalidReferenceException;
import com.example.fandoom_backend.common.exception.ResourceNotFoundException;
import com.example.fandoom_backend.media.service.ImageStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserListServiceImpl implements UserListService {

    private static final int MAX_PINNED_LISTS = 6;

    private final UserListRepository userListRepository;
    private final UserSavedItemRepository userSavedItemRepository;
    private final UserListMapper userListMapper;
    private final UserSavedItemMapper userSavedItemMapper;
    private final ImageStorageService imageStorageService;
    private final SystemListRegistry systemListRegistry;
    private final PartialUpdateValidator partialUpdateValidator;

    @Override
    @Transactional
    public List<UserListSummaryResponse> list(Long userId) {
        // WATCHLIST/READLIST sistem listeleri yoksa bu çağrıda lazy oluşturulur.
        systemListRegistry.resolve(userId, ListType.WATCHLIST);
        systemListRegistry.resolve(userId, ListType.READLIST);
        return userListRepository.findByUserIdOrderByIdAsc(userId).stream()
                .map(this::toSummary)
                .toList();
    }

    @Override
    public UserListDetailResponse getById(Long userId, Long id, Pageable pageable) {
        return toDetail(findOwned(userId, id), pageable);
    }

    @Override
    @Transactional
    public UserListDetailResponse create(Long userId, CreateUserListRequest request) {
        UserList list = UserList.builder()
                .userId(userId)
                .title(request.title())
                .description(request.description())
                .coverImageUrl(request.coverImageUrl())
                .isPublic(Boolean.TRUE.equals(request.isPublic()))
                .isPinned(false)
                .listType(ListType.CUSTOM)
                .build();
        list = userListRepository.save(list);
        return toDetail(list, Pageable.unpaged());
    }

    @Override
    @Transactional
    public UserListDetailResponse update(Long userId, Long id, CreateUserListRequest request) {
        partialUpdateValidator.validateList(request);
        UserList list = findOwned(userId, id);
        if (request.coverImageUrl() != null) {
            imageStorageService.deleteIfChanged(list.getCoverImageUrl(), request.coverImageUrl());
            list.setCoverImageUrl(request.coverImageUrl());
        }
        if (request.title() != null) {
            list.setTitle(request.title());
        }
        if (request.description() != null) {
            list.setDescription(request.description());
        }
        if (request.isPublic() != null) {
            list.setPublic(request.isPublic());
        }
        return toDetail(list, Pageable.unpaged());
    }

    @Override
    @Transactional
    public void delete(Long userId, Long id) {
        UserList list = findOwned(userId, id);
        if (list.getListType() != ListType.CUSTOM) {
            throw new InvalidReferenceException("Sistem listeleri (WATCHLIST/READLIST) silinemez");
        }
        imageStorageService.delete(list.getCoverImageUrl());
        userSavedItemRepository.deleteByUserListId(list.getId());
        userListRepository.delete(list);
    }

    @Override
    @Transactional
    public UserListSummaryResponse togglePin(Long userId, Long id) {
        UserList list = findOwned(userId, id);
        if (!list.isPinned() && userListRepository.countByUserIdAndIsPinnedTrue(userId) >= MAX_PINNED_LISTS) {
            throw new InvalidReferenceException("En fazla " + MAX_PINNED_LISTS + " liste sabitlenebilir");
        }
        list.setPinned(!list.isPinned());
        return toSummary(list);
    }

    @Override
    public List<UserListSummaryResponse> listPublicPinnedByUserId(Long userId) {
        return userListRepository.findByUserIdAndIsPublicTrueAndIsPinnedTrueOrderByIdAsc(userId).stream()
                .map(this::toSummary)
                .toList();
    }

    private UserListDetailResponse toDetail(UserList list, Pageable pageable) {
        Page<UserSavedItemResponse> items = userSavedItemRepository
                .findByUserListId(list.getId(), pageable)
                .map(userSavedItemMapper::toResponse);
        return userListMapper.toDetailResponse(list, PageResponse.from(items));
    }

    private UserListSummaryResponse toSummary(UserList list) {
        long itemCount = userSavedItemRepository.countByUserListId(list.getId());
        return userListMapper.toSummaryResponse(list, itemCount);
    }

    private UserList findOwned(Long userId, Long id) {
        return userListRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Liste bulunamadı: id=" + id));
    }
}
