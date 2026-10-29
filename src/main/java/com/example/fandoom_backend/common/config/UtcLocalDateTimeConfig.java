package com.example.fandoom_backend.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.JacksonModule;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.std.StdSerializer;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

// Uygulama UTC çalışır (bkz. FandoomBackendApplication), yani her LocalDateTime UTC "duvar saati"dir. API'de bunu
// saat dilimi belirsiz "2026-10-29T17:38:47" yerine "2026-10-29T17:38:47Z" olarak yazarız: istemci doğru anı alır ve
// kullanıcının kendi saat diliminde gösterir. Girişte "Z"li ya da "Z"siz değer kabul edilir (Jackson varsayılanı).
@Configuration
class UtcLocalDateTimeConfig {

    @Bean
    JacksonModule utcLocalDateTimeModule() {
        SimpleModule module = new SimpleModule("UtcLocalDateTimeModule");
        module.addSerializer(LocalDateTime.class, new UtcLocalDateTimeSerializer());
        return module;
    }

    static class UtcLocalDateTimeSerializer extends StdSerializer<LocalDateTime> {

        UtcLocalDateTimeSerializer() {
            super(LocalDateTime.class);
        }

        @Override
        public void serialize(LocalDateTime value, JsonGenerator gen, SerializationContext ctxt) {
            gen.writeString(DateTimeFormatter.ISO_INSTANT.format(value.toInstant(ZoneOffset.UTC)));
        }
    }
}
