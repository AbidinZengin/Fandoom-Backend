package com.example.fandoom_backend.series.dto;

import java.time.LocalDate;

public record SeasonSummaryResponse(Long id, Integer seasonNumber, String title, LocalDate airDate, String posterUrl) {}
