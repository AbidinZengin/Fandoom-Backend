package com.example.fandoom_backend.production.dto;

import java.time.LocalDate;

public record ProductionSummaryResponse(
        Long id, String slug, String title, ProductionType type,
        String posterUrl, LocalDate releaseDate) {
}
