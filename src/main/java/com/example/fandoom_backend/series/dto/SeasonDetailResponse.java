package com.example.fandoom_backend.series.dto;

import java.util.List;

public record SeasonDetailResponse(
        Long id, Integer seasonNumber, String title, String posterUrl,
        List<EpisodeResponse> episodes) {}
