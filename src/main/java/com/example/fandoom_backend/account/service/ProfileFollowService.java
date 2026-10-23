package com.example.fandoom_backend.account.service;

import com.example.fandoom_backend.account.dto.FollowStateResponse;
import com.example.fandoom_backend.account.entity.FollowState;
import com.example.fandoom_backend.common.dto.PageResponse;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.Set;

// Kişi takibi. Yaprak servis: yalnızca repository + UserService'e bağlıdır (profil verisi için
// community/'ye BAĞLANMAZ — döngü olmasın; profil zenginleştirmesi FollowListService'te).
public interface ProfileFollowService {
    // Idempotent. Kendini takip → 400, bilinmeyen username → 404.
    FollowStateResponse follow(Long followerId, String username);
    // Idempotent — takip yoksa no-op.
    FollowStateResponse unfollow(Long followerId, String username);
    FollowStateResponse getState(Long followerId, String username);

    FollowState stateOf(Long viewerId, Long userId);
    long countFollowers(Long userId);
    long countFollowing(Long userId);

    // En yeni takip önce; yalnız FOLLOWING durumundakiler.
    PageResponse<Long> listFollowerIds(Long userId, Pageable pageable);
    PageResponse<Long> listFollowingIds(Long userId, Pageable pageable);
    // ids içinden viewerId'nin takip ettikleri (liste satırlarındaki "following" bayrağı).
    Set<Long> filterFollowedBy(Long viewerId, Collection<Long> ids);
}
