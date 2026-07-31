package com.example.fandoom_backend.contentdrop.dto;

public record ContentDropSummaryResponse(
        Long id, String slug, String title, String imageUrl, String imageAlt,
        Integer readingTimeMinutes) {
}
