package com.example.fandoom_backend.series.dto;

import com.example.fandoom_backend.series.entity.EpisodeBlockType;
import com.example.fandoom_backend.series.entity.EpisodeSceneTone;

public record EpisodeBlockResponse(
        Long id, int orderIndex,
        EpisodeBlockType blockType,
        String sceneKey,
        EpisodeSceneTone tone,
        boolean pinned,
        String sceneKicker, String sceneKickerTr,
        String content, String contentTr,
        boolean lead,
        String mediaUrl,
        String mediaAlt, String mediaAltTr,
        String mediaRatio,
        String col,
        String row) {
}
