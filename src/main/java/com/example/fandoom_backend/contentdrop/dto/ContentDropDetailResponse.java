package com.example.fandoom_backend.contentdrop.dto;

import com.example.fandoom_backend.contentdrop.entity.ContentDropStatus;

import java.time.LocalDateTime;
import java.util.List;

public record ContentDropDetailResponse(
        Long id, String slug, String title, String kicker, String axis,
        String imageUrl, String imageUrlLarge, String imageAlt,
        Integer spoilerThroughSeasonNumber, Integer spoilerThroughEpisodeNumber,
        ContentDropStatus status, LocalDateTime publishedAt, long viewCount, Integer readingTimeMinutes,
        List<ContentDropBlockResponse> blocks,
        List<ContentDropTagResponse> tags,
        List<ContentDropSummaryResponse> relatedContentDrops,
        LocalDateTime createdAt, LocalDateTime updatedAt) {
}
