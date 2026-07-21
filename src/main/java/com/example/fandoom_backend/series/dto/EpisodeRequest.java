package com.example.fandoom_backend.series.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record EpisodeRequest(
        @NotNull @Positive Integer episodeNumber,
        @NotBlank @Size(max = 255) String title,
        @Size(max = 5000) String synopsis,
        LocalDate airDate,
        @Positive Integer durationMinutes,
        @Size(max = 500) String stillImageUrl) {
}
