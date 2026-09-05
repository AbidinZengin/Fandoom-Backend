package com.example.fandoom_backend.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

import java.util.List;
import java.util.Locale;

@Configuration
public class LocaleConfig {

    public static final Locale TURKISH = Locale.forLanguageTag("tr");
    public static final Locale ENGLISH = Locale.forLanguageTag("en");

    @Bean
    public LocaleResolver localeResolver() {
        AcceptHeaderLocaleResolver resolver = new AcceptHeaderLocaleResolver();
        resolver.setSupportedLocales(List.of(TURKISH, ENGLISH));
        // Accept-Language header'sız istemciler (Postman, seed/migration
        // script'leri) ENGLISH'e düşer. TURKISH varsayılanı, header
        // göndermeyen bir GET->PUT round-trip'inde LocalizedTextResolver'ın
        // ham synopsis/title yerine *Tr değerini döndürmesine ve bu yüzden
        // DB'deki İngilizce metnin kalıcı olarak Türkçe'yle ezilmesine yol
        // açıyordu (bkz. PUT /api/series/{id} synopsis bug'ı, 2026-09-02).
        resolver.setDefaultLocale(ENGLISH);
        return resolver;
    }
}
