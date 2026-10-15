package com.example.fandoom_backend.series.dto;

import java.time.LocalDate;

// Quick search projeksiyonu (MovieSearchResult ile aynı desen): iki dil ham, originalTitle'da aranır ama gösterilmez.
public record SeriesSearchResult(
        Long id, String slug, String title, String titleTr, String posterUrl, LocalDate firstAirDate) {}
