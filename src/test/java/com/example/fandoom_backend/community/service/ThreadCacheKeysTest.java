package com.example.fandoom_backend.community.service;

import com.example.fandoom_backend.community.entity.ThreadSurface;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

// Yanıtlardaki portal.name dile göre çözüldüğünden cache key'leri dile göre AYRI olmalı; aksi halde ilk isteğin dili
// herkese servis edilirdi. Portal slug'ı büyük/küçük harften bağımsız tek key'e düşer.
class ThreadCacheKeysTest {

    @AfterEach
    void resetLocale() {
        LocaleContextHolder.resetLocaleContext();
    }

    @Test
    void listKey_differsByLanguage() {
        var page = PageRequest.of(0, 20);
        LocaleContextHolder.setLocale(Locale.forLanguageTag("tr"));
        String tr = ThreadCacheKeys.list(null, "westeros", null, null, "hot", page);
        LocaleContextHolder.setLocale(Locale.ENGLISH);
        String en = ThreadCacheKeys.list(null, "westeros", null, null, "hot", page);

        assertThat(tr).isNotEqualTo(en);
    }

    @Test
    void detailKeys_differByLanguage_andByIdVersusSlug() {
        LocaleContextHolder.setLocale(Locale.forLanguageTag("tr"));
        String trId = ThreadCacheKeys.detailById(10L);
        LocaleContextHolder.setLocale(Locale.ENGLISH);
        String enId = ThreadCacheKeys.detailById(10L);

        assertThat(trId).isNotEqualTo(enId);
        assertThat(ThreadCacheKeys.detailById(10L)).isNotEqualTo(ThreadCacheKeys.detailBySlug("10"));
    }

    @Test
    void unsupportedLanguage_fallsBackToEnglishKey() {
        LocaleContextHolder.setLocale(Locale.GERMAN);
        String de = ThreadCacheKeys.detailById(1L);
        LocaleContextHolder.setLocale(Locale.ENGLISH);

        assertThat(de).isEqualTo(ThreadCacheKeys.detailById(1L));
    }

    @Test
    void portalSlug_isPartOfTheKey_caseInsensitively() {
        var page = PageRequest.of(0, 20);
        String a = ThreadCacheKeys.list(ThreadSurface.THEORY, "Westeros", null, List.of(), "hot", page);
        String b = ThreadCacheKeys.list(ThreadSurface.THEORY, "westeros", null, null, "hot", page);
        String other = ThreadCacheKeys.list(ThreadSurface.THEORY, "genel-sohbet", null, null, "hot", page);

        assertThat(a).isEqualTo(b);
        assertThat(a).isNotEqualTo(other);
        assertThat(ThreadCacheKeys.firstCursorPage(null, "westeros", null, null, "hot", 20))
                .isNotEqualTo(ThreadCacheKeys.firstCursorPage(null, null, null, null, "hot", 20));
    }
}
