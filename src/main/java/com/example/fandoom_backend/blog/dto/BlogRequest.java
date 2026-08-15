package com.example.fandoom_backend.blog.dto;

import com.example.fandoom_backend.blog.entity.BlogFormat;
import com.example.fandoom_backend.blog.entity.BlogStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

public record BlogRequest(
        @Size(max = 255) String titleTr,
        @NotBlank @Size(max = 255) String title,
        @Size(max = 255) String kickerTr,
        @Size(max = 255) String kicker,
        @Size(max = 255) String axisTr,
        @Size(max = 255) String axis,
        @Size(max = 500) String imageUrl,
        @Size(max = 500) String imageUrlLarge,
        @Size(max = 255) String imageAltTr,
        @Size(max = 255) String imageAlt,
        Integer spoilerThroughSeasonNumber,
        Integer spoilerThroughEpisodeNumber,
        Integer recommendedRank,
        boolean spoilerFree,
        @NotNull BlogStatus status,
        BlogFormat format,
        // Gonderilirse oncelikli kullanilir; bos birakilirsa mevcut
        // status-tabanli otomatik davranis (PUBLISHED'e gecince now(),
        // degilse null) korunur (bkz. BlogServiceImpl.resolvePublishedAt).
        LocalDateTime publishedAt,
        Double canvasHeight,
        List<@Valid BlogBlockRequest> blocks,
        List<@Valid BlogTagRequest> tags) {
}
