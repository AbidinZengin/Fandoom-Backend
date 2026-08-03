package com.example.fandoom_backend.blog.dto;

import java.util.List;

// Blog hub facet filtre kriterleri. Tümü opsiyoneldir; null/boş olan
// facet'ler BlogSpecificationBuilder tarafından no-op olarak ele alınır.
public record BlogFilterCriteria(
        String format,
        String franchiseSlug,
        List<String> moodSlugs,
        List<String> themeSlugs,
        Boolean spoilerFree) {
}
