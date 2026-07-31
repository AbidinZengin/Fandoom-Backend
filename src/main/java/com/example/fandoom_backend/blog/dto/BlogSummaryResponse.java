package com.example.fandoom_backend.blog.dto;

public record BlogSummaryResponse(
        Long id, String slug, String title, String imageUrl, String imageAlt,
        Integer readingTimeMinutes) {
}
