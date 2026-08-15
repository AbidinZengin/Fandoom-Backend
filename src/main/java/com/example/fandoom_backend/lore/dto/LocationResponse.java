package com.example.fandoom_backend.lore.dto;

import com.example.fandoom_backend.lore.entity.SubjectType;

public record LocationResponse(
        Long id,
        String name,
        String slug,
        String imageUrl,
        String description,
        String customFields,
        SubjectType subjectType,
        Long subjectId) {
}
