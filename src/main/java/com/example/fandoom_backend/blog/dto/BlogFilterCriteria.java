package com.example.fandoom_backend.blog.dto;

import com.example.fandoom_backend.blog.entity.BlogFormat;

import java.util.List;

// Blog hub facet filtre kriterleri. Tümü opsiyoneldir; null/boş olan
// facet'ler BlogSpecificationBuilder tarafından no-op olarak ele alınır.
public record BlogFilterCriteria(
        BlogFormat format,
        String franchiseSlug,
        List<String> moodSlugs,
        Boolean spoilerFree) {
}
