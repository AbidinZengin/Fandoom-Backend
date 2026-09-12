package com.example.fandoom_backend.account.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// Tüm alanlar opsiyonel/kısmi güncelleme — null = "değişmedi" (userId YOK,
// JWT'den @AuthenticationPrincipal ile çözülür, bkz. CLAUDE.md "Kimlik Doğrulama").
// @Size/@Pattern, Jakarta Validation spesifikasyonu gereği null değeri
// otomatik geçerli sayar — bu yüzden PATCH'te doğrudan @Valid ile kullanılır,
// ayrı bir kısmi güncelleme validator'ına gerek yoktur (bkz. AccountController).
public record UpdateUserProfileRequest(
        @Size(max = 500, message = "en fazla 500 karakter olmalı") String bio,
        @Size(max = 500, message = "en fazla 500 karakter olmalı") String avatarUrl,
        @Size(max = 500, message = "en fazla 500 karakter olmalı") String bannerUrl,
        @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "\"^#[0-9A-Fa-f]{6}$\" desenine uymalı") String accentColor,
        Boolean spoilerProtectionEnabled) {
}
