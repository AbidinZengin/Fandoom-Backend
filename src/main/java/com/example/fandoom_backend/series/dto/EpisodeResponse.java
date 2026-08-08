package com.example.fandoom_backend.series.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record EpisodeResponse(
        Long id, Integer episodeNumber, String title, String synopsis,
        LocalDate airDate, Integer durationMinutes, String stillImageUrl,
        BigDecimal externalRating, Integer externalVoteCount, LocalDateTime externalRatingUpdatedAt,
        String imdbId, Integer tmdbId,
        String storyKicker, String storyTitle, String storyThesis,
        List<EpisodeBlockResponse> episodeBlocks) {}
