package com.example.fandoom_backend.series.dto;

import com.example.fandoom_backend.series.entity.SeriesStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SeriesSummaryResponse(
        Long id, String title, String slug, String posterUrl, SeriesStatus status, LocalDate firstAirDate,
        BigDecimal externalRating) {}
