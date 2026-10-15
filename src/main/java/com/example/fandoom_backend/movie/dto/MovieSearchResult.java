package com.example.fandoom_backend.movie.dto;

import java.time.LocalDate;

// Quick search projeksiyonu: entity yüklenmez, yalnızca gösterilecek alanlar seçilir.
// İki dil de ham taşınır; hangisinin gösterileceğine search/ karar verir. originalTitle'da aranır ama gösterilmez.
public record MovieSearchResult(
        Long id, String slug, String title, String titleTr, String posterUrl, LocalDate releaseDate) {}
