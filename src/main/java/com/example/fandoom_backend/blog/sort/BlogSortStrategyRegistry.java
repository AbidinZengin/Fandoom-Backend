package com.example.fandoom_backend.blog.sort;

import com.example.fandoom_backend.common.exception.InvalidReferenceException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class BlogSortStrategyRegistry {

    private final Map<String, BlogSortStrategy> byKey;

    public BlogSortStrategyRegistry(List<BlogSortStrategy> strategies) {
        this.byKey = strategies.stream()
                .collect(Collectors.toMap(BlogSortStrategy::key, Function.identity()));
    }

    public Sort resolve(String key) {
        BlogSortStrategy strategy = byKey.get(key);
        if (strategy == null) {
            throw new InvalidReferenceException("Geçersiz sort değeri: " + key);
        }
        return strategy.toJpaSort();
    }
}
