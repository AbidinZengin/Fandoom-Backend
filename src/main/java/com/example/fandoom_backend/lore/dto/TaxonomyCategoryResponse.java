package com.example.fandoom_backend.lore.dto;

import com.example.fandoom_backend.lore.entity.SubjectType;

public record TaxonomyCategoryResponse(
        Long id,
        String name,
        String slug,
        SubjectType subjectType,
        Long subjectId) {
}
