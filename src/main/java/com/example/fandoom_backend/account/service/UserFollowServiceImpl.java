package com.example.fandoom_backend.account.service;

import com.example.fandoom_backend.account.dto.FollowStatusResponse;
import com.example.fandoom_backend.account.dto.UserFollowResponse;
import com.example.fandoom_backend.account.entity.SavedItemType;
import com.example.fandoom_backend.account.entity.UserFollow;
import com.example.fandoom_backend.account.repository.UserFollowRepository;
import com.example.fandoom_backend.common.dto.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserFollowServiceImpl implements UserFollowService {

    private final UserFollowRepository userFollowRepository;
    private final ItemReferenceValidator itemReferenceValidator;

    @Override
    @Transactional
    public FollowStatusResponse follow(Long userId, SavedItemType itemType, Long itemId) {
        boolean alreadyFollowing = userFollowRepository
                .findByUserIdAndItemTypeAndItemId(userId, itemType, itemId).isPresent();
        if (!alreadyFollowing) {
            itemReferenceValidator.assertExists(itemType, itemId);
            userFollowRepository.save(UserFollow.builder()
                    .userId(userId).itemType(itemType).itemId(itemId).build());
        }
        return buildStatus(userId, itemType, itemId);
    }

    @Override
    @Transactional
    public FollowStatusResponse unfollow(Long userId, SavedItemType itemType, Long itemId) {
        userFollowRepository.findByUserIdAndItemTypeAndItemId(userId, itemType, itemId)
                .ifPresent(userFollowRepository::delete);
        return buildStatus(userId, itemType, itemId);
    }

    @Override
    public PageResponse<UserFollowResponse> list(Long userId, Pageable pageable) {
        return PageResponse.from(userFollowRepository.findByUserId(userId, pageable)
                .map(this::toResponse));
    }

    @Override
    public long count(SavedItemType itemType, Long itemId) {
        return userFollowRepository.countByItemTypeAndItemId(itemType, itemId);
    }

    private FollowStatusResponse buildStatus(Long userId, SavedItemType itemType, Long itemId) {
        boolean following = userFollowRepository.existsByUserIdAndItemTypeAndItemId(userId, itemType, itemId);
        long count = userFollowRepository.countByItemTypeAndItemId(itemType, itemId);
        return new FollowStatusResponse(following, count);
    }

    private UserFollowResponse toResponse(UserFollow follow) {
        return new UserFollowResponse(follow.getId(), follow.getItemId(), follow.getItemType(), follow.getCreatedAt());
    }
}
