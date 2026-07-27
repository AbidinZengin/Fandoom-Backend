package com.example.fandoom_backend.tag.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TagRequest(
        @NotBlank @Size(max = 60) String name,
        @Size(max = 2000) String description) {
}
