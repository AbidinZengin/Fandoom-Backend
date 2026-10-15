package com.example.fandoom_backend.search.service;

// search/ modülünün cache adları. TTL global (1 saat, RedisConfig) — yeni içerik aramada 1 saate kadar geç görünebilir
// (kabul edildi). Yazma yolu yok, evict yok: bayatlık yalnızca TTL ile sınırlı.
public final class SearchCacheNames {

    public static final String QUICK = "search:quick";

    private SearchCacheNames() {
    }
}
