package com.example.fandoom_backend.community.service;

// Thread cache adları (RedisConfig'te tek global TTL). Yazma yolları bu iki cache'i birlikte temizler.
public final class ThreadCacheNames {

    public static final String DETAIL = "thread:detail";
    public static final String LIST = "thread:list";

    private ThreadCacheNames() {
    }
}
