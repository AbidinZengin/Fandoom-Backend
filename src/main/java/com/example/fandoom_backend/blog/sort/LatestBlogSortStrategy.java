package com.example.fandoom_backend.blog.sort;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

@Component
public class LatestBlogSortStrategy implements BlogSortStrategy {

    @Override
    public String key() {
        return "latest";
    }

    @Override
    public Sort toJpaSort() {
        return Sort.by(Sort.Direction.DESC, "publishedAt");
    }
}
