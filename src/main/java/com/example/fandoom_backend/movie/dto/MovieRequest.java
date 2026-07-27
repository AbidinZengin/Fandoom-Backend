package com.example.fandoom_backend.movie.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

public record MovieRequest(
        @NotBlank @Size(max = 255) String title,
        @Size(max = 255) String originalTitle,
        @Size(max = 5000) String synopsis,
        LocalDate releaseDate,
        @Positive Integer runtimeMinutes,
        @Size(max = 500) String posterUrl,
        @Size(max = 500) String coverImageUrl,
        @Size(max = 500) String trailerUrl,
        @Size(max = 10) String contentRating,
        @Size(max = 2) String originCountry,
        @Size(max = 2) String originalLanguage,
        @DecimalMin("0.0") @DecimalMax("10.0") BigDecimal externalRating,
        @PositiveOrZero Integer externalVoteCount,
        @Size(max = 15) String imdbId,
        @Positive Integer tmdbId,
        Long franchiseId,
        Set<Long> genreIds,
        Set<Long> producerIds) {
}
