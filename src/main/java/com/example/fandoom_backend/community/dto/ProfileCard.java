package com.example.fandoom_backend.community.dto;

// Liste satırları için profilin hafif özeti (toplu çözüm: UserProfileService.getCardsByUserIds).
// isPrivate: gizli profilin bio/accentColor'ı yalnız sahibine ve onaylı takipçilere gösterilir (avatar vitrindir,
// her zaman görünür) — liste üreten servisler bunu uygulamak zorundadır.
public record ProfileCard(String avatarUrl, String bio, String accentColor, boolean isPrivate) {
}
