package com.example.fandoom_backend.blog.sort;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

@Component
public class TrendingBlogSortStrategy implements BlogSortStrategy {

    // TODO (v2): zaman-ağırlıklı "trending" formülü (ör. viewCount'u
    // publishedAt'e göre yarı-ömürlü azaltan bir skor) burada hesaplanacak.
    // v1.5 kapsamında ertelendi, şimdilik basit viewCount sıralaması yeterli.

    @Override
    public String key() {
        return "trending";
    }

    @Override
    public Sort toJpaSort() {
        return Sort.by(Sort.Direction.DESC, "viewCount");
    }
}
