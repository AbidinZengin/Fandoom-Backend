package com.example.fandoom_backend.series.dto;

import com.example.fandoom_backend.series.entity.SeriesStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

public record SeriesDetailResponse(
        Long id, String title, String originalTitle, String slug, String synopsis,
        LocalDate firstAirDate, LocalDate lastAirDate, SeriesStatus status,
        String posterUrl, String coverImageUrl, String trailerUrl,
        String contentRating, String originCountry, String originalLanguage,
        BigDecimal externalRating, Integer externalVoteCount, LocalDateTime externalRatingUpdatedAt,
        String imdbId, Integer tmdbId,
        Long franchiseId, Set<Long> genreIds, Set<Long> producerIds,
        List<SeasonSummaryResponse> seasons,
        LocalDateTime createdAt, LocalDateTime updatedAt) {}
