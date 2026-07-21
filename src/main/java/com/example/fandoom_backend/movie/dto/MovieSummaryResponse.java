package com.example.fandoom_backend.movie.dto;

import java.time.LocalDate;

public record MovieSummaryResponse(Long id, String title, String slug, String posterUrl, LocalDate releaseDate) {}
