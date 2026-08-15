package com.example.fandoom_backend.lore.dto;

import com.example.fandoom_backend.lore.entity.SubjectType;

public record EventResponse(
        Long id,
        String name,
        String description,
        Integer orderIndex,
        String imageUrl,
        SubjectType subjectType,
        Long subjectId,
        Long locationId,
        String locationName,
        String locationSlug,
        boolean pinned,
        String customFields) {
}
