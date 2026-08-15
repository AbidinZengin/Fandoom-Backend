package com.example.fandoom_backend.lore.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TaxonomyCategoryRequest(
        @NotBlank @Size(max = 150) String name) {
}
