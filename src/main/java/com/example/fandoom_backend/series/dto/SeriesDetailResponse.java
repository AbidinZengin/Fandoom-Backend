package com.example.fandoom_backend.series.dto;

import com.example.fandoom_backend.series.entity.SeriesStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

public record SeriesDetailResponse(
        Long id, String title, String slug, String synopsis,
        LocalDate firstAirDate, SeriesStatus status,
        String posterUrl, String coverImageUrl,
        Long franchiseId, Set<Long> genreIds,
        List<SeasonSummaryResponse> seasons,
        LocalDateTime createdAt, LocalDateTime updatedAt) {}
