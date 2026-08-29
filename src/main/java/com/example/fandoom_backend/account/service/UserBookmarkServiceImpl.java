package com.example.fandoom_backend.account.service;

import com.example.fandoom_backend.account.dto.BookmarkStatusResponse;
import com.example.fandoom_backend.account.dto.UserBookmarkResponse;
import com.example.fandoom_backend.account.entity.ActivityType;
import com.example.fandoom_backend.account.entity.SavedItemType;
import com.example.fandoom_backend.account.entity.UserBookmark;
import com.example.fandoom_backend.account.repository.UserBookmarkRepository;
import com.example.fandoom_backend.common.dto.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserBookmarkServiceImpl implements UserBookmarkService {

    private final UserBookmarkRepository userBookmarkRepository;
    private final ItemReferenceValidator itemReferenceValidator;
    private final ActivityLogService activityLogService;

    @Override
    @Transactional
    public BookmarkStatusResponse bookmark(Long userId, SavedItemType itemType, Long itemId) {
        boolean alreadyBookmarked = userBookmarkRepository
                .findByUserIdAndItemTypeAndItemId(userId, itemType, itemId).isPresent();
        if (!alreadyBookmarked) {
            itemReferenceValidator.assertExists(itemType, itemId);
            userBookmarkRepository.save(UserBookmark.builder()
                    .userId(userId).itemType(itemType).itemId(itemId).build());
            activityLogService.record(userId, ActivityType.BOOKMARKED, itemId, itemType);
        }
        return buildStatus(userId, itemType, itemId);
    }

    @Override
    @Transactional
    public BookmarkStatusResponse unbookmark(Long userId, SavedItemType itemType, Long itemId) {
        userBookmarkRepository.findByUserIdAndItemTypeAndItemId(userId, itemType, itemId)
                .ifPresent(userBookmarkRepository::delete);
        return buildStatus(userId, itemType, itemId);
    }

    @Override
    public BookmarkStatusResponse getStatus(Long userId, SavedItemType itemType, Long itemId) {
        return buildStatus(userId, itemType, itemId);
    }

    @Override
    public PageResponse<UserBookmarkResponse> list(Long userId, Pageable pageable) {
        return PageResponse.from(userBookmarkRepository.findByUserId(userId, pageable)
                .map(this::toResponse));
    }

    @Override
    public long count(SavedItemType itemType, Long itemId) {
        return userBookmarkRepository.countByItemTypeAndItemId(itemType, itemId);
    }

    private BookmarkStatusResponse buildStatus(Long userId, SavedItemType itemType, Long itemId) {
        boolean bookmarked = userBookmarkRepository.existsByUserIdAndItemTypeAndItemId(userId, itemType, itemId);
        long count = userBookmarkRepository.countByItemTypeAndItemId(itemType, itemId);
        return new BookmarkStatusResponse(bookmarked, count);
    }

    private UserBookmarkResponse toResponse(UserBookmark bookmark) {
        return new UserBookmarkResponse(bookmark.getId(), bookmark.getItemId(), bookmark.getItemType(), bookmark.getCreatedAt());
    }
}
