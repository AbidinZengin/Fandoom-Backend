package com.example.fandoom_backend.person.dto;

import com.example.fandoom_backend.person.entity.SubjectType;

public record CastResponse(
        Long id,
        PersonSummaryResponse person,
        CharacterResponse character,
        SubjectType subjectType,
        Long subjectId,
        Integer billingOrder) {
}
