package com.example.fandoom_backend.blog.dto;

import com.example.fandoom_backend.blog.entity.BlogBlockType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record BlogBlockRequest(
        @NotNull BlogBlockType blockType,
        @Size(max = 5000) String text,
        @Size(max = 500) String imageUrl,
        @Size(max = 255) String imageAlt) {
}
