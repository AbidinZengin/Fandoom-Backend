package com.example.fandoom_backend.series.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record SeasonRequest(
        @NotNull @Positive Integer seasonNumber,
        @Size(max = 255) String title,
        @Size(max = 500) String posterUrl) {
}
