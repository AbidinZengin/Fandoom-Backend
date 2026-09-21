package com.example.fandoom_backend.community.dto;

import com.example.fandoom_backend.community.entity.ThreadSurface;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

// tags: ham/karışık-case string olarak gelebilir, servis SlugGenerator.slugify
// ile normalize eder (ör. "Game Of Thrones" -> "game-of-thrones") — ayrı bir
// regex/format doğrulaması burada YOK, tag/ modülünün "hataya açık" deneyimini
// tekrarlamamak için bilinçli olarak esnek tutuldu.
// media: en fazla 6 (görsel+video karışık), sıra = liste sırası. imageUrl DEPRECATED:
// media boşsa/yoksa tek elemanlı media[IMAGE] gibi işlenir, media doluysa yok sayılır.
// portalSlug ZORUNLU: her thread tek bir portala aittir (portal yok -> 404, ARCHIVED -> 400, STAFF_ONLY -> 403;
// productionSlug verildiyse portalın yapımlarından biri olmalı).
public record ThreadRequest(
        @NotNull ThreadSurface surface,
        @NotBlank @Size(min = 10, max = 200) String title,
        @Size(max = 10000) String body,
        @Size(max = 500) String imageUrl,
        boolean spoilerFlagged,
        @Size(max = 255) String productionSlug,
        @Size(max = 10) List<@NotBlank @Size(max = 100) String> tags,
        @Size(max = 6) List<@Valid @NotNull ThreadMediaRequest> media,
        @NotBlank @Size(max = 50) String portalSlug) {
}
