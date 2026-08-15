package com.example.fandoom_backend.lore.dto;

import com.example.fandoom_backend.lore.entity.SubjectType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TaxonomyCategoryRequest(
        @NotBlank @Size(max = 150) String name,
        // Sadece generic POST /api/lore/categories icin: nested
        // /api/movies|series/{id}/lore/categories cagrilarinda path'ten gelir.
        SubjectType subjectType,
        Long subjectId) {
}
