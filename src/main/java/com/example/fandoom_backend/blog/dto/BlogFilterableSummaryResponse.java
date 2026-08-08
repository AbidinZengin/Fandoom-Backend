package com.example.fandoom_backend.blog.dto;

import com.example.fandoom_backend.blog.entity.BlogFormat;

import java.time.LocalDateTime;
import java.util.List;

// Blog hub kartı için özet gösterim: mevcut BlogSummaryResponse'dan bağımsız,
// facet bilgileriyle (format/franchiseSlug/moods/spoilerFree)
// zenginleştirilmiş ayrı bir DTO.
public record BlogFilterableSummaryResponse(
        Long id, String slug, String title, String imageUrl, String imageAlt,
        Integer readingTimeMinutes, LocalDateTime publishedAt, long viewCount,
        BlogFormat format, String franchiseSlug, List<String> moods,
        boolean spoilerFree) {
}
