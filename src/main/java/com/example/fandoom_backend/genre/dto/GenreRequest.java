package com.example.fandoom_backend.genre.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GenreRequest(
        @Size(max = 80) String nameTr,
        @NotBlank @Size(max = 80) String name) {
}
