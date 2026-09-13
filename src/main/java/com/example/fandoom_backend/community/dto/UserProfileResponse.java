package com.example.fandoom_backend.community.dto;

// id: hedef kullanıcının User.id'si (username DEĞİL) — frontend kişi takibi
// icin POST /api/me/follows/USER/{id} çağrısında bunu kullanır.
// followerCount/followingCount/isFollowing: account/UserFollowService
// (itemType=USER) üzerinden hesaplanır, bkz. UserProfileServiceImpl.
public record UserProfileResponse(
        Long id,
        String username,
        String bio,
        String avatarUrl,
        String bannerUrl,
        String accentColor,
        boolean spoilerProtectionEnabled,
        ProfileStats stats,
        long followerCount,
        long followingCount,
        boolean isFollowing) {
}
