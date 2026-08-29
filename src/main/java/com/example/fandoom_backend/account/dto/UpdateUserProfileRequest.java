package com.example.fandoom_backend.account.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// Tüm alanlar opsiyonel/kısmi güncelleme — null = "değişmedi" (userId YOK,
// JWT'den @AuthenticationPrincipal ile çözülür, bkz. CLAUDE.md "Kimlik Doğrulama").
public record UpdateUserProfileRequest(
        @Size(max = 500) String bio,
        @Size(max = 500) String avatarUrl,
        @Size(max = 500) String bannerUrl,
        @Pattern(regexp = "^#[0-9A-Fa-f]{6}$") String accentColor,
        Boolean spoilerProtectionEnabled) {
}
