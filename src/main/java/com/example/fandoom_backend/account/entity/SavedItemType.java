package com.example.fandoom_backend.account.entity;

// UserSavedItem/UserLike/UserFollow/UserActivityLog arasında paylaşılan ortak
// hedef tipi. Cross-module referans: sadece bu enum + düz Long id, JPA
// ilişkisi YOK (CLAUDE.md cross-module ID-only kuralı).
// USER: kişi takibi (follower/following) — sadece UserFollow ile kullanılır
// (Like/Bookmark/SavedItem'da anlamsız, ama şişkin bir "sadece bazı enum
// değerleri bazı servislerde geçerli" ayrımı ölçek küçükken bilinçli olarak
// yapılmadı — ItemReferenceValidator zaten switch ile her tip için ayrı
// doğrulama yapıyor).
public enum SavedItemType {
    MOVIE, SERIES, BLOG, USER
}
