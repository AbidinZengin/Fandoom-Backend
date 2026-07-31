package com.example.fandoom_backend.blog.dto;

import com.example.fandoom_backend.blog.entity.BlogBlockType;

public record BlogBlockResponse(
        Long id, int orderIndex, BlogBlockType blockType,
        String text, String imageUrl, String imageAlt) {
}
