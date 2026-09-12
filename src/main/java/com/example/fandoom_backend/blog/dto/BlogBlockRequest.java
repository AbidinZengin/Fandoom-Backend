package com.example.fandoom_backend.blog.dto;

import com.example.fandoom_backend.blog.entity.BlogBlockType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record BlogBlockRequest(
        @NotNull BlogBlockType blockType,
        @NotBlank @Size(max = 100) String sceneKey,
        String contentTr,
        String content,
        @Size(max = 500) String imageUrl,
        @Size(max = 255) String imageAltTr,
        @Size(max = 255) String imageAlt) {
}
