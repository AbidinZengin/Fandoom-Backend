package com.example.fandoom_backend.blog.sort;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

@Component
public class OldestBlogSortStrategy implements BlogSortStrategy {

    @Override
    public String key() {
        return "oldest";
    }

    @Override
    public Sort toJpaSort() {
        return Sort.by(Sort.Direction.ASC, "publishedAt");
    }
}
