package com.example.fandoom_backend.community.dto;

import com.example.fandoom_backend.account.entity.FollowState;

// id: hedef kullanıcının User.id'si (username DEĞİL).
// followerCount/followingCount: account/ProfileFollowService üzerinden, bkz. UserProfileServiceImpl.
// following == (followStatus == FOLLOWING). isFollowing eski FE alanıdır, aynı değeri taşır (geriye uyumluluk).
// isSelf: izleyici profil sahibi. restricted: profil gizli, izleyici sahibi değil ve onaylı takipçi değil —
// bu durumda bio/accentColor/stats içerik alanları null döner (bkz. withViewer).
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
        boolean isFollowing,
        boolean following,
        boolean isSelf,
        boolean isPrivate,
        boolean restricted,
        FollowState followStatus) {

    // Mapper izleyiciden bağımsız alanları doldurur; izleyiciye bağlı alanlar burada eklenir.
    public UserProfileResponse withViewer(boolean isSelf, FollowState followStatus, boolean restricted) {
        boolean followingNow = followStatus == FollowState.FOLLOWING;
        if (restricted) {
            return new UserProfileResponse(id, username, null, avatarUrl, bannerUrl, null, false, null,
                    followerCount, followingCount, followingNow, followingNow, isSelf, isPrivate, true,
                    followStatus);
        }
        return new UserProfileResponse(id, username, bio, avatarUrl, bannerUrl, accentColor,
                spoilerProtectionEnabled, stats, followerCount, followingCount, followingNow, followingNow,
                isSelf, isPrivate, false, followStatus);
    }
}
