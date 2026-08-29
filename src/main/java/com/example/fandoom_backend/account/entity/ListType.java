package com.example.fandoom_backend.account.entity;

// CUSTOM: kullanıcının kendi açtığı liste (silinebilir, tipi asla değişmez).
// WATCHLIST/READLIST: her kullanıcı için ilk ihtiyaç anında lazy oluşturulan,
// silinemeyen/tipi değiştirilemeyen sistem listeleri.
public enum ListType {
    CUSTOM, WATCHLIST, READLIST
}
