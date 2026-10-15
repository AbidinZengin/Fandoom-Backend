package com.example.fandoom_backend.community.dto;

// Quick search projeksiyonu: iki dil ham taşınır, hangisinin gösterileceğine search/ karar verir.
public record PortalSearchResult(Long id, String slug, String nameTr, String nameEn, String iconUrl) {}
