package com.example.fandoom_backend.movie.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.Set;

public record MovieRequest(
        @NotBlank @Size(max = 255) String title,
        @Size(max = 5000) String synopsis,
        LocalDate releaseDate,
        @Positive Integer runtimeMinutes,
        @Size(max = 500) String posterUrl,
        @Size(max = 500) String coverImageUrl,
        Long franchiseId,
        Set<Long> genreIds) {
}
