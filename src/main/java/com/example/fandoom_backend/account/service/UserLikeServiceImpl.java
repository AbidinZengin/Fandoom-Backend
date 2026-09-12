package com.example.fandoom_backend.account.service;

import com.example.fandoom_backend.account.dto.LikeStatusResponse;
import com.example.fandoom_backend.account.dto.UserLikeResponse;
import com.example.fandoom_backend.account.entity.ActivityType;
import com.example.fandoom_backend.account.entity.SavedItemType;
import com.example.fandoom_backend.account.entity.UserLike;
import com.example.fandoom_backend.account.repository.UserLikeRepository;
import com.example.fandoom_backend.common.dto.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserLikeServiceImpl implements UserLikeService {

    private final UserLikeRepository userLikeRepository;
    private final ItemReferenceValidator itemReferenceValidator;
    private final ActivityLogService activityLogService;

    @Override
    @Transactional
    public LikeStatusResponse like(Long userId, SavedItemType itemType, Long itemId) {
        boolean alreadyLiked = userLikeRepository
                .findByUserIdAndItemTypeAndItemId(userId, itemType, itemId).isPresent();
        if (!alreadyLiked) {
            itemReferenceValidator.assertExists(itemType, itemId);
            userLikeRepository.save(UserLike.builder()
                    .userId(userId).itemType(itemType).itemId(itemId).build());
            activityLogService.record(userId, ActivityType.LIKED, itemId, itemType);
        }
        return buildStatus(userId, itemType, itemId);
    }

    @Override
    @Transactional
    public LikeStatusResponse unlike(Long userId, SavedItemType itemType, Long itemId) {
        userLikeRepository.findByUserIdAndItemTypeAndItemId(userId, itemType, itemId)
                .ifPresent(userLikeRepository::delete);
        return buildStatus(userId, itemType, itemId);
    }

    @Override
    public LikeStatusResponse getStatus(Long userId, SavedItemType itemType, Long itemId) {
        return buildStatus(userId, itemType, itemId);
    }

    @Override
    public PageResponse<UserLikeResponse> list(Long userId, Pageable pageable) {
        return PageResponse.from(userLikeRepository.findByUserId(userId, pageable)
                .map(this::toResponse));
    }

    @Override
    public long count(SavedItemType itemType, Long itemId) {
        return userLikeRepository.countByItemTypeAndItemId(itemType, itemId);
    }

    @Override
    public long countByUserId(Long userId) {
        return userLikeRepository.countByUserId(userId);
    }

    private LikeStatusResponse buildStatus(Long userId, SavedItemType itemType, Long itemId) {
        boolean liked = userLikeRepository.existsByUserIdAndItemTypeAndItemId(userId, itemType, itemId);
        long count = userLikeRepository.countByItemTypeAndItemId(itemType, itemId);
        return new LikeStatusResponse(liked, count);
    }

    private UserLikeResponse toResponse(UserLike like) {
        return new UserLikeResponse(like.getId(), like.getItemId(), like.getItemType(), like.getCreatedAt());
    }
}
