package com.example.fandoom_backend.lore.dto;

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
        @Size(max = 10000) String customFields) {
}
