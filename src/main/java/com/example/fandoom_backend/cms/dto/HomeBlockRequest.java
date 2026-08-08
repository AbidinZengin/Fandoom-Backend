package com.example.fandoom_backend.cms.dto;

import com.example.fandoom_backend.cms.entity.ContentType;
import com.example.fandoom_backend.cms.entity.PageName;
import com.example.fandoom_backend.cms.entity.SectionName;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record HomeBlockRequest(
        @NotNull PageName page,
        Long entityId,
        @NotNull SectionName section,
        @NotNull ContentType contentType,
        @NotBlank @Size(max = 2000) String contentValue,
        @Size(max = 2000) String linkUrl,
        @Size(max = 255) String altText,
        boolean active,
        int orderIndex,
        @Size(max = 20) String col,
        @Size(max = 20) String row) {
}
