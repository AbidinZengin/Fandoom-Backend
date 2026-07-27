package com.example.fandoom_backend.series.dto;

import java.time.LocalDate;
import java.util.List;

public record SeasonDetailResponse(
        Long id, Integer seasonNumber, String title, LocalDate airDate, String posterUrl,
        List<EpisodeResponse> episodes) {}
