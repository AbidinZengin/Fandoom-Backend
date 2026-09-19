package com.example.fandoom_backend.common.config;

import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.LoggingCacheErrorHandler;
import org.springframework.context.annotation.Configuration;

// Redis erişilemezse (kesinti/timeout) cache hataları isteği 500'e düşürmesin: hata loglanır ve
// metod DB'den çalışır (cache bir optimizasyondur, doğruluk kaynağı değil). RedisConfig'den ayrı
// tutuldu: CachingConfigurer bean'leri erken oluşturulur, cacheManager @Bean'ini taşımamalı.
@Configuration
public class CacheErrorConfig implements CachingConfigurer {

    @Override
    public CacheErrorHandler errorHandler() {
        return new LoggingCacheErrorHandler(false);
    }
}
