package com.example.fandoom_backend.common.util;

import org.springframework.context.i18n.LocaleContextHolder;

import java.util.Locale;

/**
 * İstek dili tr ise trValue, değilse enValue döner; seçilen alan boşsa
 * diğer dile düşer (TMDB gibi kaynaklardan eksik gelen içerik için savunma katmanı).
 */
public final class LocalizedTextResolver {

    private LocalizedTextResolver() {
    }

    public static String resolve(String trValue, String enValue) {
        return resolve(trValue, enValue, LocaleContextHolder.getLocale());
    }

    public static String resolve(String trValue, String enValue, Locale locale) {
        boolean turkish = locale != null && "tr".equalsIgnoreCase(locale.getLanguage());
        String primary = turkish ? trValue : enValue;
        String fallback = turkish ? enValue : trValue;
        return (primary != null && !primary.isBlank()) ? primary : fallback;
    }
}
