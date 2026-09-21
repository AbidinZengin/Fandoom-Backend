package com.example.fandoom_backend.community.dto;

import com.example.fandoom_backend.community.entity.PortalPostingPolicy;
import com.example.fandoom_backend.community.entity.PortalStatus;

import java.time.LocalDateTime;
import java.util.List;

// PortalSummaryResponse + createdAt.
public record PortalDetailResponse(
        Long id, String slug, String name, String description, String bannerUrl, String iconUrl,
        String accentColor,
        int memberCount, int threadCount, boolean isMember, PortalPostingPolicy postingPolicy,
        PortalStatus status, List<String> productionSlugs, LocalDateTime createdAt) {
}
