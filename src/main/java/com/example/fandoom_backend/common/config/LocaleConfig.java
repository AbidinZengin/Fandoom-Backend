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
        resolver.setDefaultLocale(TURKISH);
        return resolver;
    }
}
