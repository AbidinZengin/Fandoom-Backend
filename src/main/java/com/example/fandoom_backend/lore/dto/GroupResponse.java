package com.example.fandoom_backend.lore.dto;

import com.example.fandoom_backend.lore.entity.SubjectType;

public record GroupResponse(
        Long id,
        String name,
        String slug,
        String imageUrl,
        Long categoryId,
        String categoryName,
        String categorySlug,
        SubjectType subjectType,
        Long subjectId,
        String customFields) {
}
