package com.example.fandoom_backend.blog.dto;

import com.example.fandoom_backend.blog.entity.BlogStatus;

public record BlogSummaryResponse(
        Long id, String slug, String title, String imageUrl, String imageAlt,
        Integer readingTimeMinutes, BlogStatus status) {
}
