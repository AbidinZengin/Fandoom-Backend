package com.example.fandoom_backend.community.dto;

// Liste satırları için profilin hafif özeti (toplu çözüm: UserProfileService.getCardsByUserIds).
public record ProfileCard(String avatarUrl, String bio, String accentColor) {
}
