package com.example.fandoom_backend.community.dto;

import com.example.fandoom_backend.community.entity.ThreadSurface;

import java.time.LocalDateTime;
import java.util.List;

public record ThreadDetailResponse(
        Long id, String slug, ThreadSurface surface, String title, String body, String imageUrl,
        boolean spoilerFlagged, Long authorId, String productionSlug,
        int likeCount, int commentCount, int bookmarkCount,
        List<String> tags, LocalDateTime createdAt, LocalDateTime updatedAt) {
}
