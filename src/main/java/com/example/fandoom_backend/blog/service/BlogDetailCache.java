package com.example.fandoom_backend.blog.service;

import com.example.fandoom_backend.blog.dto.BlogDetailResponse;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

// BlogServiceImpl.getBySlug'ın YAN ETKİLERİ var (viewCount artırma + READ_BLOG aktivite kaydı);
// metodun kendisine @Cacheable koymak cache-hit'te bu yan etkileri atlardı. Bu yüzden yalnızca
// "detayı yükle" kısmı burada cache'lenir, yan etkiler her istekte çağıran tarafta çalışır.
// Ayrı bir bean olmasının sebebi: aynı sınıf içinden çağrı Spring proxy'sini atlar (self-invocation).
// key yalnızca slug'dan üretilir; loader key'e girmez.
@Component
public class BlogDetailCache {

    @Cacheable(cacheNames = BlogCacheNames.DETAIL, key = "'slug:' + #slug", sync = true)
    public BlogDetailResponse getOrLoad(String slug, Supplier<BlogDetailResponse> loader) {
        return loader.get();
    }
}
