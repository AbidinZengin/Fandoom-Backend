package com.example.fandoom_backend.series.dto;

import com.example.fandoom_backend.series.entity.SeasonBlockType;

public record SeasonBlockResponse(
        Long id, int orderIndex,
        SeasonBlockType blockType,
        String sceneKey,
        String content, String contentTr,
        Integer mediaEpisodeRef,
        String mediaCaption, String mediaCaptionTr,
        String mediaCredit) {
}
