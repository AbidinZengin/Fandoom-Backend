package com.example.fandoom_backend.series.dto;

import com.example.fandoom_backend.series.entity.SeriesStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.Set;

public record SeriesRequest(
        @NotBlank @Size(max = 255) String title,
        @Size(max = 5000) String synopsis,
        LocalDate firstAirDate,
        @NotNull SeriesStatus status,
        @Size(max = 500) String posterUrl,
        @Size(max = 500) String coverImageUrl,
        Long franchiseId,
        Set<Long> genreIds) {
}
