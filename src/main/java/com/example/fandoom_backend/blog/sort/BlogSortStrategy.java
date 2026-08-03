package com.example.fandoom_backend.blog.sort;

import org.springframework.data.domain.Sort;

// Blog hub'daki her sıralama seçeneği (Latest/Oldest/Trending/Recommended)
// bu arayüzün ayrı bir implementasyonudur (Open/Closed: yeni bir sıralama
// seçeneği mevcut kodu değiştirmeden yeni bir @Component ile eklenir).
public interface BlogSortStrategy {
    String key();
    Sort toJpaSort();
}
