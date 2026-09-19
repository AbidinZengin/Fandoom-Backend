package com.example.fandoom_backend.community.dto;

import com.example.fandoom_backend.community.entity.ThreadMediaType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// Sıra = istekteki liste sırası; position servis tarafında atanır.
public record ThreadMediaRequest(
        @NotNull ThreadMediaType type,
        @NotBlank @Size(max = 500) String url) {
}
