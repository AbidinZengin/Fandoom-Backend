package com.example.fandoom_backend.franchise.dto;

import java.time.LocalDateTime;

public record FranchiseDetailResponse(
        Long id, String name, String slug, String description,
        String logoUrl, String bannerImageUrl,
        LocalDateTime createdAt, LocalDateTime updatedAt) {}
