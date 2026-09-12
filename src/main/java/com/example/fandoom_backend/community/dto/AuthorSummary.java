package com.example.fandoom_backend.community.dto;

// username çözülemezse (ör. silinmiş kullanıcı) username null gelir — thread/
// comment'in kendisi silinmez, sadece yazar bilgisi eksik döner.
public record AuthorSummary(Long id, String username) {
}
