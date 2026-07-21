package com.example.fandoom_backend.common.util;

import java.text.Normalizer;
import java.util.Locale;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public final class SlugGenerator {

    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^a-z0-9]+");
    private static final Pattern EDGE_DASHES = Pattern.compile("^-+|-+$");

    private SlugGenerator() {
    }

    public static String slugify(String input) {
        String normalized = input
                .replace("ı", "i").replace("İ", "i")
                .replace("ğ", "g").replace("Ğ", "g")
                .replace("ü", "u").replace("Ü", "u")
                .replace("ş", "s").replace("Ş", "s")
                .replace("ö", "o").replace("Ö", "o")
                .replace("ç", "c").replace("Ç", "c");
        normalized = Normalizer.normalize(normalized, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        // Locale.ROOT şart: JVM varsayılan locale'i tr-TR ise "I".toLowerCase()
        // "i" değil "ı" üretir, bu da alfanumerik sayılmayıp silinirdi (ör.
        // "Interstellar" -> "nterstellar").
        String slug = NON_ALPHANUMERIC.matcher(normalized.toLowerCase(Locale.ROOT)).replaceAll("-");
        return EDGE_DASHES.matcher(slug).replaceAll("");
    }

    public static String generateUnique(String base, Predicate<String> existsBySlug) {
        String candidate = slugify(base);
        if (!existsBySlug.test(candidate)) {
            return candidate;
        }
        int suffix = 2;
        String numbered;
        do {
            numbered = candidate + "-" + suffix;
            suffix++;
        } while (existsBySlug.test(numbered));
        return numbered;
    }
}
