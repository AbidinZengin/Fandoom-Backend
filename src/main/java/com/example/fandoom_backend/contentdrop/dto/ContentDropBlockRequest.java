package com.example.fandoom_backend.contentdrop.dto;

import com.example.fandoom_backend.contentdrop.entity.ContentDropBlockType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ContentDropBlockRequest(
        @NotNull ContentDropBlockType blockType,
        @Size(max = 5000) String text,
        @Size(max = 500) String imageUrl,
        @Size(max = 255) String imageAlt) {
}
