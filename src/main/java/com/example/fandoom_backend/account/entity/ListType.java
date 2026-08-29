package com.example.fandoom_backend.account.entity;

// CUSTOM: kullanıcının kendi açtığı liste (silinebilir, tipi asla değişmez).
// WATCHLIST/READLIST/WATCHED: her kullanıcı için ilk ihtiyaç anında lazy
// oluşturulan, silinemeyen/tipi değiştirilemeyen sistem listeleri. WATCHED,
// WATCHLIST'ten bağımsızdır — bir öğe "izlendi" işaretlense de WATCHLIST'te
// kalmaya devam eder (kullanıcı kararıyla: otomatik çıkarma YOK).
public enum ListType {
    CUSTOM, WATCHLIST, READLIST, WATCHED
}
