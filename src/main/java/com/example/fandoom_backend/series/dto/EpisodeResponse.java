package com.example.fandoom_backend.series.dto;

import java.time.LocalDate;

public record EpisodeResponse(
        Long id, Integer episodeNumber, String title, String synopsis,
        LocalDate airDate, Integer durationMinutes, String stillImageUrl) {}
