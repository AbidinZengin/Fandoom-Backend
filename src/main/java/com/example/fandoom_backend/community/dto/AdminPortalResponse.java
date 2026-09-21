package com.example.fandoom_backend.community.dto;

import com.example.fandoom_backend.community.entity.PortalPostingPolicy;
import com.example.fandoom_backend.community.entity.PortalStatus;

import java.time.LocalDateTime;
import java.util.List;

// Admin görünümü: locale çözümlemesi YOK — düzenleme formu için ham TR/EN alanları döner, HIDDEN dahil.
public record AdminPortalResponse(
        Long id, String slug, String nameTr, String nameEn, String descriptionTr, String descriptionEn,
        String bannerUrl, String iconUrl, String accentColor, PortalStatus status, PortalPostingPolicy postingPolicy, int sortOrder,
        int memberCount, int threadCount, List<String> productionSlugs,
        LocalDateTime createdAt, LocalDateTime updatedAt) {
}
