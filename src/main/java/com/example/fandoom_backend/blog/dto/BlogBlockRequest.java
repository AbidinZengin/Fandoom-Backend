package com.example.fandoom_backend.blog.dto;

import com.example.fandoom_backend.blog.entity.BlockAnimation;
import com.example.fandoom_backend.blog.entity.BlockFontFamily;
import com.example.fandoom_backend.blog.entity.BlogBlockType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record BlogBlockRequest(
        @NotNull BlogBlockType blockType,
        @Size(max = 5000) String textTr,
        @Size(max = 5000) String text,
        @Size(max = 500) String imageUrl,
        @Size(max = 255) String imageAltTr,
        @Size(max = 255) String imageAlt,
        @NotNull Double x,
        @NotNull Double y,
        @NotNull Double width,
        Double height,
        BlockAnimation animation,
        BlockFontFamily fontFamily,
        Double fontScale) {
}
