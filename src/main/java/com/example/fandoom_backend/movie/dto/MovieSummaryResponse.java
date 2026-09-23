package com.example.fandoom_backend.movie.dto;

import com.example.fandoom_backend.movie.entity.MovieStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MovieSummaryResponse(
        Long id, String title, String titleTr, String slug, String posterUrl,
        LocalDate releaseDate, BigDecimal externalRating, MovieStatus status) {}
