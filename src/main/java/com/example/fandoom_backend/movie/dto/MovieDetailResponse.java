package com.example.fandoom_backend.movie.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

public record MovieDetailResponse(
        Long id, String title, String slug, String synopsis,
        LocalDate releaseDate, Integer runtimeMinutes,
        String posterUrl, String coverImageUrl,
        Long franchiseId, Set<Long> genreIds,
        LocalDateTime createdAt, LocalDateTime updatedAt) {}
