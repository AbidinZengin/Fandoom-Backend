package com.example.fandoom_backend.community.service;

import com.example.fandoom_backend.common.util.SlugGenerator;
import com.example.fandoom_backend.community.entity.ThreadSurface;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

// @Cacheable key üretimi (SpEL: T(...ThreadCacheKeys).list(...)). Aynı sonucu veren istekler AYNI
// key'e düşsün diye sort/tags normalize edilir — aksi halde ?sort=rastgele gibi girdiler aynı
// içerik için sınırsız sayıda ayrı cache girdisi (cache pollution) üretirdi.
public final class ThreadCacheKeys {

    private ThreadCacheKeys() {
    }

    public static String list(ThreadSurface surface, String productionSlug, List<String> tags, String sort,
                              Pageable pageable) {
        return "list:" + filter(surface, productionSlug, tags, sort)
                + ":" + pageable.getPageNumber() + ":" + pageable.getPageSize();
    }

    // Yalnızca ilk sayfa cache'lenir (cursor == null) — sonraki cursor'lar sınırsız çeşitlilikte.
    public static String firstCursorPage(ThreadSurface surface, String productionSlug, List<String> tags,
                                         String sort, int size) {
        return "cursor:" + filter(surface, productionSlug, tags, sort) + ":" + size;
    }

    private static String filter(ThreadSurface surface, String productionSlug, List<String> tags, String sort) {
        String normalizedTags = tags == null ? "" : tags.stream()
                .filter(Objects::nonNull)
                .map(SlugGenerator::slugify)
                .filter(t -> !t.isBlank())
                .distinct()
                .sorted()
                .collect(Collectors.joining(","));
        return surface + ":" + productionSlug + ":" + normalizedTags + ":" + normalizeSort(sort);
    }

    private static String normalizeSort(String sort) {
        return "new".equals(sort) || "top".equals(sort) ? sort : "hot";
    }
}
