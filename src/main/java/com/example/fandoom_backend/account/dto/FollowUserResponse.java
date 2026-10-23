package com.example.fandoom_backend.account.dto;

// Takipçi/takip edilen listesi satırı. following: isteği yapan oturumun bu kullanıcıyı takip edip etmediği.
public record FollowUserResponse(String username, String avatarUrl, String bio, String accentColor,
                                 boolean following) {
}
