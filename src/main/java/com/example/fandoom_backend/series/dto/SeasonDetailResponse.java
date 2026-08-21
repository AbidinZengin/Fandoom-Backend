package com.example.fandoom_backend.series.dto;

import java.time.LocalDate;
import java.util.List;

public record SeasonDetailResponse(
        Long id, Integer seasonNumber, String title, String titleTr,
        LocalDate airDate, String posterUrl,
        String storyKicker, String storyKickerTr,
        String storyTitle, String storyTitleTr,
        String storyDek, String storyDekTr,
        List<EpisodeResponse> episodes,
        List<SeasonBlockResponse> seasonBlocks) {}
