package com.example.fandoom_backend.cms.dto;

import com.example.fandoom_backend.cms.entity.ContentType;
import com.example.fandoom_backend.cms.entity.PageName;
import com.example.fandoom_backend.cms.entity.SectionName;

public record HomeBlockResponse(
        Long id,
        PageName page,
        Long entityId,
        SectionName section,
        ContentType contentType,
        String contentValue,
        String linkUrl,
        String altText,
        boolean active,
        int orderIndex,
        String col,
        String row) {
}
