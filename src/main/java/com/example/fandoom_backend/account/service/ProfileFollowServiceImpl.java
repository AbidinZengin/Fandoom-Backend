package com.example.fandoom_backend.account.service;

import com.example.fandoom_backend.account.dto.FollowStateResponse;
import com.example.fandoom_backend.account.entity.FollowState;
import com.example.fandoom_backend.account.event.ProfileFollowEvent;
import com.example.fandoom_backend.account.entity.ProfileFollow;
import com.example.fandoom_backend.account.repository.ProfileFollowRepository;
import com.example.fandoom_backend.common.dto.PageResponse;
import com.example.fandoom_backend.common.exception.InvalidReferenceException;
import com.example.fandoom_backend.common.exception.ResourceNotFoundException;
import com.example.fandoom_backend.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProfileFollowServiceImpl implements ProfileFollowService {

    private final ProfileFollowRepository profileFollowRepository;
    private final UserService userService;
    private final ProfilePrivacyLookup profilePrivacyLookup;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public FollowStateResponse follow(Long followerId, String username) {
        Long followedId = userService.getIdByUsername(username);
        if (followedId.equals(followerId)) {
            throw new InvalidReferenceException("Kendi kendinizi takip edemezsiniz");
        }
        if (profileFollowRepository.findByFollowerIdAndFollowedId(followerId, followedId).isEmpty()) {
            // Gizli profilde takip kurulmaz, onay bekler.
            FollowState initial = profilePrivacyLookup.isPrivate(followedId)
                    ? FollowState.REQUESTED : FollowState.FOLLOWING;
            // Eşzamanlı çift istekte yalnız kazanan 1 döner; kaybeden sessizce no-op (idempotent), olay yayınlamaz.
            int inserted = profileFollowRepository.insertIgnore(followerId, followedId, initial.name(),
                    LocalDateTime.now());
            if (inserted > 0) {
                eventPublisher.publishEvent(new ProfileFollowEvent(
                        initial == FollowState.REQUESTED
                                ? ProfileFollowEvent.Type.REQUESTED : ProfileFollowEvent.Type.FOLLOWED,
                        followerId, followedId));
            }
        }
        return buildState(followerId, followedId);
    }

    @Override
    @Transactional
    public FollowStateResponse unfollow(Long followerId, String username) {
        Long followedId = userService.getIdByUsername(username);
        profileFollowRepository.findByFollowerIdAndFollowedId(followerId, followedId).ifPresent(row -> {
            profileFollowRepository.delete(row);
            eventPublisher.publishEvent(new ProfileFollowEvent(
                    ProfileFollowEvent.Type.WITHDRAWN, followerId, followedId));
        });
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
    public Map<Long, FollowState> statesOf(Long viewerId, Collection<Long> userIds) {
        if (viewerId == null || userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        return profileFollowRepository.findByFollowerIdAndFollowedIdIn(viewerId, userIds).stream()
                .collect(Collectors.toMap(ProfileFollow::getFollowedId, ProfileFollow::getStatus));
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

    @Override
    public boolean isRestricted(Long viewerId, Long userId) {
        if (viewerId != null && viewerId.equals(userId)) {
            return false;
        }
        return profilePrivacyLookup.isPrivate(userId) && stateOf(viewerId, userId) != FollowState.FOLLOWING;
    }

    @Override
    public PageResponse<Long> listRequesterIds(Long userId, Pageable pageable) {
        return PageResponse.from(profileFollowRepository.findFollowerIds(userId, FollowState.REQUESTED, pageable));
    }

    @Override
    @Transactional
    public void acceptRequest(Long userId, String requesterUsername) {
        Long requesterId = userService.getIdByUsername(requesterUsername);
        ProfileFollow row = profileFollowRepository.findByFollowerIdAndFollowedId(requesterId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Takip isteği bulunamadı: " + requesterUsername));
        if (row.getStatus() == FollowState.REQUESTED) {
            row.setStatus(FollowState.FOLLOWING);
            eventPublisher.publishEvent(new ProfileFollowEvent(
                    ProfileFollowEvent.Type.ACCEPTED, userId, requesterId));
        }
    }

    @Override
    @Transactional
    public void rejectRequest(Long userId, String requesterUsername) {
        Long requesterId = userService.getIdByUsername(requesterUsername);
        profileFollowRepository.findByFollowerIdAndFollowedId(requesterId, userId)
                .filter(row -> row.getStatus() == FollowState.REQUESTED)
                .ifPresent(row -> {
                    profileFollowRepository.delete(row);
                    // Reddetme istek sahibine bildirim üretmez; yalnız profil sahibindeki bayat istek bildirimi silinir.
                    eventPublisher.publishEvent(new ProfileFollowEvent(
                            ProfileFollowEvent.Type.REQUEST_REJECTED, userId, requesterId));
                });
    }

    @Override
    @Transactional
    public void removeFollower(Long userId, String followerUsername) {
        Long followerId = userService.getIdByUsername(followerUsername);
        profileFollowRepository.findByFollowerIdAndFollowedId(followerId, userId)
                .filter(row -> row.getStatus() == FollowState.FOLLOWING)
                .ifPresent(profileFollowRepository::delete);
    }

    @Override
    @Transactional
    public int approveAllRequests(Long userId) {
        // Toplu UPDATE olay üretmez; her istek sahibine "kabul edildi" gitsin diye satırlar tek tek onaylanır.
        List<ProfileFollow> pending = profileFollowRepository.findByFollowedIdAndStatus(userId, FollowState.REQUESTED);
        for (ProfileFollow row : pending) {
            row.setStatus(FollowState.FOLLOWING);
            eventPublisher.publishEvent(new ProfileFollowEvent(
                    ProfileFollowEvent.Type.ACCEPTED, userId, row.getFollowerId()));
        }
        return pending.size();
    }

    private FollowStateResponse buildState(Long followerId, Long followedId) {
        FollowState state = stateOf(followerId, followedId);
        return new FollowStateResponse(state == FollowState.FOLLOWING, state, countFollowers(followedId));
    }
}
