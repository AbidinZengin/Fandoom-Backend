package com.example.fandoom_backend.blog.dto;

import com.example.fandoom_backend.blog.entity.BlogFormat;
import com.example.fandoom_backend.blog.entity.BlogStatus;

import java.time.LocalDateTime;
import java.util.List;

public record BlogDetailResponse(
        Long id, String slug,
        String title, String titleTr,
        String kicker, String kickerTr,
        String axis, String axisTr,
        String imageUrl, String imageUrlLarge,
        String imageAlt, String imageAltTr,
        Integer spoilerThroughSeasonNumber, Integer spoilerThroughEpisodeNumber,
        Integer recommendedRank, boolean spoilerFree,
        BlogStatus status, BlogFormat format, LocalDateTime publishedAt, long viewCount, Integer readingTimeMinutes,
        List<BlogBlockResponse> blocks,
        List<BlogTagResponse> tags,
        List<BlogSummaryResponse> relatedBlogs,
        LocalDateTime createdAt, LocalDateTime updatedAt) {
}
