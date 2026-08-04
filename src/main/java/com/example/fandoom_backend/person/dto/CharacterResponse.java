package com.example.fandoom_backend.person.dto;

import com.example.fandoom_backend.person.entity.SubjectType;

public record CharacterResponse(
        Long id,
        String name,
        String slug,
        String description,
        String quote,
        String imageUrl,
        SubjectType subjectType,
        Long subjectId,
        Integer billingOrder) {
}
