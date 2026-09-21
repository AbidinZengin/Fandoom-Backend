package com.example.fandoom_backend.community.dto;

import com.example.fandoom_backend.community.entity.ThreadSurface;

import java.time.LocalDateTime;
import java.util.List;

// portal: thread'in ait olduğu portal ({slug, name}, name istek diline göre). imageUrl: geriye uyumluluk — ilk IMAGE medyanın url'i (yoksa null). Yeni istemciler media'yı kullanmalı.
public record ThreadDetailResponse(
        Long id, String slug, ThreadSurface surface, String title, String body, String imageUrl,
        List<ThreadMediaResponse> media,
        boolean spoilerFlagged, Long authorId, AuthorSummary author, String productionSlug,
        int likeCount, int commentCount, int bookmarkCount,
        boolean isLiked, boolean isBookmarked,
        List<String> tags, LocalDateTime createdAt, LocalDateTime updatedAt, PortalRefResponse portal) {
}
