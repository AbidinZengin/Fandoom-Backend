package com.example.fandoom_backend.contentdrop.dto;

import com.example.fandoom_backend.contentdrop.entity.ContentDropBlockType;

public record ContentDropBlockResponse(
        Long id, int orderIndex, ContentDropBlockType blockType,
        String text, String imageUrl, String imageAlt) {
}
