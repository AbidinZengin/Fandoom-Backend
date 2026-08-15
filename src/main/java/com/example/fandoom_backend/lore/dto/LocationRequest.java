package com.example.fandoom_backend.lore.dto;

import com.example.fandoom_backend.lore.entity.SubjectType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LocationRequest(
        @NotBlank @Size(max = 150) String name,
        @Size(max = 500) String imageUrl,
        @Size(max = 5000) String description,
        @Size(max = 10000) String customFields,
        // Sadece generic POST /api/lore/locations icin: nested
        // /api/movies|series/{id}/lore/locations cagrilarinda path'ten gelir.
        SubjectType subjectType,
        Long subjectId) {
}
