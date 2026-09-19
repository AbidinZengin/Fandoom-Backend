package com.example.fandoom_backend.common.util;

import com.example.fandoom_backend.common.exception.InvalidReferenceException;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

// Keyset sayfalamanın "son görülen satır" işaretçisi: sıralama alanının değeri
// + tie-breaker olarak id. Client'a Base64URL("değer|id") opak string olarak gider.
public record KeysetCursor(String value, Long id) {

    private static final char SEPARATOR = '|';

    public String encode() {
        String raw = value + SEPARATOR + id;
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    // null/boş cursor = "ilk sayfa" → null döner.
    public static KeysetCursor decode(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return null;
        }
        try {
            String raw = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
            int idx = raw.lastIndexOf(SEPARATOR);
            if (idx <= 0) {
                throw new IllegalArgumentException("separator yok");
            }
            return new KeysetCursor(raw.substring(0, idx), Long.parseLong(raw.substring(idx + 1)));
        } catch (IllegalArgumentException e) {
            throw new InvalidReferenceException("Geçersiz cursor");
        }
    }
}
