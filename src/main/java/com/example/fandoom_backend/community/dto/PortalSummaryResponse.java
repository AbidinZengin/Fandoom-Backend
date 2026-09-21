package com.example.fandoom_backend.community.dto;

import com.example.fandoom_backend.community.entity.PortalPostingPolicy;
import com.example.fandoom_backend.community.entity.PortalStatus;

import java.util.List;

// name/description istek diline göre çözülmüş (yoksa TR<->EN fallback). isMember anonimde false.
public record PortalSummaryResponse(
        Long id, String slug, String name, String description, String bannerUrl, String iconUrl,
        String accentColor,
        int memberCount, int threadCount, boolean isMember, PortalPostingPolicy postingPolicy,
        PortalStatus status, List<String> productionSlugs) {
}
