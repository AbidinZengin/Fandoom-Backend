package com.example.fandoom_backend.community.service;

import com.example.fandoom_backend.common.util.SlugGenerator;
import com.example.fandoom_backend.community.entity.ThreadSurface;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

// @Cacheable key üretimi (SpEL: T(...ThreadCacheKeys).list(...)). Aynı sonucu veren istekler AYNI
// key'e düşsün diye sort/tags normalize edilir — aksi halde ?sort=rastgele gibi girdiler aynı
// içerik için sınırsız sayıda ayrı cache girdisi (cache pollution) üretirdi.
// Yanıtlardaki portal.name istek diline göre çözüldüğünden (LocalizedTextResolver) her key'e dil (tr|en) eklenir:
// aksi halde ilk isteğin dili tüm dillere servis edilirdi.
public final class ThreadCacheKeys {

    private ThreadCacheKeys() {
    }

    public static String list(ThreadSurface surface, String portalSlug, String productionSlug, List<String> tags,
                              String sort, Pageable pageable) {
        return "list:" + filter(surface, portalSlug, productionSlug, tags, sort)
                + ":" + pageable.getPageNumber() + ":" + pageable.getPageSize();
    }

    // Yalnızca ilk sayfa cache'lenir (cursor == null) — sonraki cursor'lar sınırsız çeşitlilikte.
    public static String firstCursorPage(ThreadSurface surface, String portalSlug, String productionSlug,
                                         List<String> tags, String sort, int size) {
        return "cursor:" + filter(surface, portalSlug, productionSlug, tags, sort) + ":" + size;
    }

    public static String detailBySlug(String slug) {
        return "slug:" + slug + ":" + language();
    }

    public static String detailById(Long id) {
        return "id:" + id + ":" + language();
    }

    private static String filter(ThreadSurface surface, String portalSlug, String productionSlug, List<String> tags,
                                 String sort) {
        String normalizedTags = tags == null ? "" : tags.stream()
                .filter(Objects::nonNull)
                .map(SlugGenerator::slugify)
                .filter(t -> !t.isBlank())
                .distinct()
                .sorted()
                .collect(Collectors.joining(","));
        // Portal slug'ı küçük harfe normalize (DB collation büyük/küçük harf duyarsız: aynı portal, tek key).
        String normalizedPortal = portalSlug == null ? null : portalSlug.toLowerCase(Locale.ROOT);
        return surface + ":" + normalizedPortal + ":" + productionSlug + ":" + normalizedTags + ":"
                + normalizeSort(sort) + ":" + language();
    }

    private static String normalizeSort(String sort) {
        return "new".equals(sort) || "top".equals(sort) ? sort : "hot";
    }

    private static String language() {
        return "tr".equalsIgnoreCase(LocaleContextHolder.getLocale().getLanguage()) ? "tr" : "en";
    }
}
