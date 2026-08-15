package com.example.fandoom_backend.lore.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record GroupRequest(
        @NotBlank @Size(max = 150) String name,
        @Size(max = 500) String imageUrl,
        @NotNull Long categoryId,
        @Size(max = 10000) String customFields) {
}
