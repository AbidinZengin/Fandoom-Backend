package com.example.fandoom_backend.series.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record EpisodeRequest(
        @NotNull @Positive Integer episodeNumber,
        @NotBlank @Size(max = 255) String title,
        @Size(max = 5000) String synopsis,
        LocalDate airDate,
        @Positive Integer durationMinutes,
        @Size(max = 500) String stillImageUrl,
        @DecimalMin("0.0") @DecimalMax("10.0") BigDecimal externalRating,
        @PositiveOrZero Integer externalVoteCount,
        @Size(max = 15) String imdbId,
        @Positive Integer tmdbId) {
}
