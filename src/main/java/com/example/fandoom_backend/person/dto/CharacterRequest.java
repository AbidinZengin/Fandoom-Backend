package com.example.fandoom_backend.person.dto;

import com.example.fandoom_backend.person.entity.SubjectType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CharacterRequest(
        @NotBlank @Size(max = 150) String name,
        @Size(max = 5000) String description,
        @Size(max = 500) String quote,
        @Size(max = 500) String imageUrl,
        @Positive Integer billingOrder,
        // Sadece generic POST /api/characters icin: nested
        // /api/movies|series/{id}/characters cagrilarinda path'ten gelir, body'de
        // gerekmez.
        SubjectType subjectType,
        Long subjectId) {
}
