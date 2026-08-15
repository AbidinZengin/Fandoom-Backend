package com.example.fandoom_backend.lore.dto;

import com.example.fandoom_backend.lore.entity.SubjectType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record EventRequest(
        @NotBlank @Size(max = 150) String name,
        String description,
        @NotNull Integer orderIndex,
        @Size(max = 500) String imageUrl,
        Long locationId,
        boolean pinned,
        @Size(max = 10000) String customFields,
        // Sadece generic POST /api/lore/events icin: nested
        // /api/movies|series/{id}/lore/events cagrilarinda path'ten gelir.
        SubjectType subjectType,
        Long subjectId) {
}
