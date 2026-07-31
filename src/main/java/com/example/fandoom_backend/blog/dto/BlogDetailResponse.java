package com.example.fandoom_backend.blog.dto;

import com.example.fandoom_backend.blog.entity.BlogStatus;

import java.time.LocalDateTime;
import java.util.List;

public record BlogDetailResponse(
        Long id, String slug, String title, String kicker, String axis,
        String imageUrl, String imageUrlLarge, String imageAlt,
        Integer spoilerThroughSeasonNumber, Integer spoilerThroughEpisodeNumber,
        BlogStatus status, LocalDateTime publishedAt, long viewCount, Integer readingTimeMinutes,
        List<BlogBlockResponse> blocks,
        List<BlogTagResponse> tags,
        List<BlogSummaryResponse> relatedBlogs,
        LocalDateTime createdAt, LocalDateTime updatedAt) {
}
