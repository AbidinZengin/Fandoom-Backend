package com.example.fandoom_backend.account.entity;

// UserSavedItem/UserLike/UserFollow/UserActivityLog arasında paylaşılan ortak
// hedef tipi. Cross-module referans: sadece bu enum + düz Long id, JPA
// ilişkisi YOK (CLAUDE.md cross-module ID-only kuralı).
// Kişi takibi bu enum'da DEĞİL: durum (FOLLOWING/REQUESTED) taşıdığı ve Like/Bookmark/SavedItem'da
// anlamsız olduğu için ayrı tabloda (bkz. ProfileFollow, V4 migration).
public enum SavedItemType {
    MOVIE, SERIES, BLOG
}
