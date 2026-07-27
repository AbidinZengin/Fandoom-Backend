package com.example.fandoom_backend.tag.dto;

import com.example.fandoom_backend.tag.entity.TaggableType;
import jakarta.validation.constraints.NotNull;

public record TagAssignmentRequest(
        @NotNull TaggableType taggableType,
        @NotNull Long taggableId) {
}
