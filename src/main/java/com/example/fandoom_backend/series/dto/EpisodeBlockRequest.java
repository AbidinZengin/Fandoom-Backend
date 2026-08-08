package com.example.fandoom_backend.series.dto;

import com.example.fandoom_backend.series.entity.EpisodeBlockType;
import com.example.fandoom_backend.series.entity.EpisodeSceneTone;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record EpisodeBlockRequest(
        @NotNull EpisodeBlockType blockType,
        @NotBlank @Size(max = 100) String sceneKey,
        EpisodeSceneTone tone,
        boolean pinned,
        @Size(max = 255) String sceneKicker,
        String content,
        boolean lead,
        @Size(max = 500) String mediaUrl,
        @Size(max = 255) String mediaAlt,
        @Size(max = 20) String mediaRatio,
        @Size(max = 20) String col,
        @Size(max = 20) String row) {
}
