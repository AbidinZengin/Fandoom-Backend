package com.example.fandoom_backend.series.dto;

import com.example.fandoom_backend.series.entity.SeasonBlockType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SeasonBlockRequest(
        @NotNull SeasonBlockType blockType,
        @NotBlank @Size(max = 100) String sceneKey,
        String contentTr,
        String content,
        Integer mediaEpisodeRef,
        @Size(max = 255) String mediaCaptionTr,
        @Size(max = 255) String mediaCaption,
        @Size(max = 255) String mediaCredit) {
}
