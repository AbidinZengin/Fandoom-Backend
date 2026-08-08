package com.example.fandoom_backend.group.dto;

import com.example.fandoom_backend.group.entity.TaggableType;
import jakarta.validation.constraints.NotNull;

public record GroupAssignmentRequest(
        @NotNull TaggableType taggableType,
        @NotNull Long taggableId) {
}
