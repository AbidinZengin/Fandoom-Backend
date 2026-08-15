package com.example.fandoom_backend.lore.dto;

import com.example.fandoom_backend.lore.entity.TaggableType;
import jakarta.validation.constraints.NotNull;

public record GroupAssignmentRequest(
        @NotNull TaggableType taggableType,
        @NotNull Long taggableId) {
}
