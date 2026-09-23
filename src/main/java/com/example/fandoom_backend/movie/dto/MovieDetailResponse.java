package com.example.fandoom_backend.movie.dto;

import com.example.fandoom_backend.movie.entity.MovieStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

public record MovieDetailResponse(
        Long id, String title, String titleTr,
        String originalTitle, String slug,
        String synopsis, String synopsisTr,
        String tagline, String taglineTr,
        MovieStatus status, Long budget, Long boxOffice,
        LocalDate releaseDate, Integer runtimeMinutes,
        String posterUrl, String coverImageUrl, String trailerUrl,
        String contentRating, String originCountry, String originalLanguage,
        BigDecimal externalRating, Integer externalVoteCount, LocalDateTime externalRatingUpdatedAt,
        String imdbId, Integer tmdbId,
        Long franchiseId, Set<Long> genreIds, Set<Long> producerIds,
        Set<Long> directorIds, Set<Long> writerIds,
        LocalDateTime createdAt, LocalDateTime updatedAt) {}
