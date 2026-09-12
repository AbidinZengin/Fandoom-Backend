package com.example.fandoom_backend.community.dto;

// username çözülemezse (ör. silinmiş kullanıcı) username null gelir — thread/
// comment'in kendisi silinmez, sadece yazar bilgisi eksik döner. avatarUrl:
// kullanıcı hiç profil oluşturmamışsa (UserProfile satırı yok) null gelir,
// hata fırlatılmaz (bkz. UserProfileService.getAvatarUrlsByUserIds).
public record AuthorSummary(Long id, String username, String avatarUrl) {
}
