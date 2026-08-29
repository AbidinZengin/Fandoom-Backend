package com.example.fandoom_backend.account.dto;

public record UserProfileResponse(
        String username,
        String bio,
        String avatarUrl,
        String bannerUrl,
        String accentColor,
        boolean spoilerProtectionEnabled,
        ProfileStats stats) {
}
