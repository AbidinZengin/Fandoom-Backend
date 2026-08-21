package com.example.fandoom_backend.series.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public record SeasonRequest(
        @NotNull @Positive Integer seasonNumber,
        @Size(max = 255) String titleTr,
        @Size(max = 255) String title,
        LocalDate airDate,
        @Size(max = 500) String posterUrl,
        @Size(max = 255) String storyKickerTr,
        @Size(max = 255) String storyKicker,
        @Size(max = 255) String storyTitleTr,
        @Size(max = 255) String storyTitle,
        String storyDekTr,
        String storyDek,
        List<SeasonBlockRequest> seasonBlocks) {
}
