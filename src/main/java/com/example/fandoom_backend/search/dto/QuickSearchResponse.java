package com.example.fandoom_backend.search.dto;

import java.util.List;

// Sonucu olmayan türün grubu listede yer almaz; gruplar SearchType sırasındadır.
public record QuickSearchResponse(List<SearchGroup> groups) {

    public static QuickSearchResponse empty() {
        return new QuickSearchResponse(List.of());
    }
}
