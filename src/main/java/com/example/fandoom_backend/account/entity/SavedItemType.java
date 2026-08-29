package com.example.fandoom_backend.account.entity;

// UserSavedItem/UserLike/UserFollow/UserActivityLog arasında paylaşılan ortak
// hedef tipi. Cross-module referans: sadece bu enum + düz Long id, JPA
// ilişkisi YOK (CLAUDE.md cross-module ID-only kuralı).
public enum SavedItemType {
    MOVIE, SERIES, BLOG
}
