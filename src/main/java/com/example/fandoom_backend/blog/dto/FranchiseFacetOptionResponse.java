package com.example.fandoom_backend.blog.dto;

// Blog hub facet paneli için: bir franchise'ın gösterim alanları
// (FranchiseSummaryResponse/FranchiseDetailResponse'dan bağımsız, sadece
// facet panelinin ihtiyaç duyduğu alt küme) + o franchise'a etiketli
// distinct blog sayısı.
public record FranchiseFacetOptionResponse(Long id, String name, String slug, String logoUrl, long count) {}
