package com.example.fandoom_backend.group.dto;

import com.example.fandoom_backend.group.entity.GroupType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record GroupRequest(
        @NotBlank @Size(max = 150) String name,
        @Size(max = 500) String imageUrl,
        @NotNull GroupType type) {
}
