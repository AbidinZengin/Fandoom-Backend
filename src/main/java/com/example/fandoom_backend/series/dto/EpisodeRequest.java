package com.example.fandoom_backend.series.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record EpisodeRequest(
        @NotNull @Positive Integer episodeNumber,
        @Size(max = 255) String titleTr,
        @NotBlank @Size(max = 255) String title,
        @Size(max = 5000) String synopsisTr,
        @Size(max = 5000) String synopsis,
        LocalDate airDate,
        @Positive Integer durationMinutes,
        @Size(max = 500) String stillImageUrl,
        @DecimalMin("0.0") @DecimalMax("10.0") BigDecimal externalRating,
        @PositiveOrZero Integer externalVoteCount,
        @Size(max = 15) String imdbId,
        @Positive Integer tmdbId,
        @Size(max = 255) String storyKickerTr,
        @Size(max = 255) String storyKicker,
        @Size(max = 255) String storyTitleTr,
        @Size(max = 255) String storyTitle,
        String storyThesisTr,
        String storyThesis,
        @Valid List<EpisodeBlockRequest> episodeBlocks) {
}
