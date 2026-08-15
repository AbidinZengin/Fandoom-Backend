package com.example.fandoom_backend.blog.dto;

import com.example.fandoom_backend.blog.entity.BlockAnimation;
import com.example.fandoom_backend.blog.entity.BlockFontFamily;
import com.example.fandoom_backend.blog.entity.BlogBlockType;

public record BlogBlockResponse(
        Long id, int orderIndex, BlogBlockType blockType,
        String text, String textTr,
        String imageUrl, String imageAlt, String imageAltTr,
        Double x, Double y, Double width, Double height,
        BlockAnimation animation, BlockFontFamily fontFamily, Double fontScale) {
}
