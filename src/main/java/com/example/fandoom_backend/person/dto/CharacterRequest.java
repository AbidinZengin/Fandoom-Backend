package com.example.fandoom_backend.person.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CharacterRequest(
        @NotBlank @Size(max = 150) String name,
        @Size(max = 5000) String description,
        @Size(max = 500) String imageUrl) {
}
