package com.example.fandoom_backend.series.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record EpisodeResponse(
        Long id, Integer episodeNumber, String title, String synopsis,
        LocalDate airDate, Integer durationMinutes, String stillImageUrl,
        BigDecimal externalRating, Integer externalVoteCount, LocalDateTime externalRatingUpdatedAt,
        String imdbId, Integer tmdbId) {}
