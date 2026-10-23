package com.example.fandoom_backend.account.service;

import com.example.fandoom_backend.account.dto.FollowStateResponse;
import com.example.fandoom_backend.account.entity.FollowState;
import com.example.fandoom_backend.account.entity.ProfileFollow;
import com.example.fandoom_backend.account.repository.ProfileFollowRepository;
import com.example.fandoom_backend.common.dto.PageResponse;
import com.example.fandoom_backend.common.exception.InvalidReferenceException;
import com.example.fandoom_backend.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProfileFollowServiceImpl implements ProfileFollowService {

    private final ProfileFollowRepository profileFollowRepository;
    private final UserService userService;

    @Override
    @Transactional
    public FollowStateResponse follow(Long followerId, String username) {
        Long followedId = userService.getIdByUsername(username);
        if (followedId.equals(followerId)) {
            throw new InvalidReferenceException("Kendi kendinizi takip edemezsiniz");
        }
        if (profileFollowRepository.findByFollowerIdAndFollowedId(followerId, followedId).isEmpty()) {
            try {
                profileFollowRepository.saveAndFlush(ProfileFollow.builder()
                        .followerId(followerId).followedId(followedId).status(FollowState.FOLLOWING).build());
            } catch (DataIntegrityViolationException e) {
                // Eşzamanlı çift istek: unique kısıt ikincisini reddeder, sonuç zaten "takip ediliyor" (idempotent).
            }
        }
        return buildState(followerId, followedId);
    }

    @Override
    @Transactional
    public FollowStateResponse unfollow(Long followerId, String username) {
        Long followedId = userService.getIdByUsername(username);
        profileFollowRepository.findByFollowerIdAndFollowedId(followerId, followedId)
                .ifPresent(profileFollowRepository::delete);
        return buildState(followerId, followedId);
    }

    @Override
    public FollowStateResponse getState(Long followerId, String username) {
        return buildState(followerId, userService.getIdByUsername(username));
    }

    @Override
    public FollowState stateOf(Long viewerId, Long userId) {
        if (viewerId == null) {
            return FollowState.NONE;
        }
        return profileFollowRepository.findByFollowerIdAndFollowedId(viewerId, userId)
                .map(ProfileFollow::getStatus)
                .orElse(FollowState.NONE);
    }

    @Override
    public long countFollowers(Long userId) {
        return profileFollowRepository.countByFollowedIdAndStatus(userId, FollowState.FOLLOWING);
    }

    @Override
    public long countFollowing(Long userId) {
        return profileFollowRepository.countByFollowerIdAndStatus(userId, FollowState.FOLLOWING);
    }

    @Override
    public PageResponse<Long> listFollowerIds(Long userId, Pageable pageable) {
        return PageResponse.from(profileFollowRepository.findFollowerIds(userId, FollowState.FOLLOWING, pageable));
    }

    @Override
    public PageResponse<Long> listFollowingIds(Long userId, Pageable pageable) {
        return PageResponse.from(profileFollowRepository.findFollowedIds(userId, FollowState.FOLLOWING, pageable));
    }

    @Override
    public Set<Long> filterFollowedBy(Long viewerId, Collection<Long> ids) {
        if (viewerId == null || ids == null || ids.isEmpty()) {
            return Set.of();
        }
        return profileFollowRepository.findFollowedIdsAmong(viewerId, FollowState.FOLLOWING, ids);
    }

    private FollowStateResponse buildState(Long followerId, Long followedId) {
        FollowState state = stateOf(followerId, followedId);
        return new FollowStateResponse(state == FollowState.FOLLOWING, state, countFollowers(followedId));
    }
}
