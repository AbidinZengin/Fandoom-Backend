package com.example.fandoom_backend.trivia.entity;

import java.util.Locale;

/**
 * Hazır (predefined) kategoriler — FE bunlara özel ikon çizer. Trivia'nın etiketleri
 * serbest metin de olabilir; bu enum bir kısıt değil, "tanınan değerler" kümesidir
 * (DB'de etiketler düz String saklanır).
 */
public enum TriviaTag {
    BEHIND_THE_SCENES,
    EASTER_EGG,
    CASTING,
    GOOF,
    LORE;

    /**
     * Boşlukları sadeleştirir; hazır bir değere denk geliyorsa (büyük/küçük harf, boşluk, tire fark etmez)
     * kanonik ada (ör. "behind the scenes" → BEHIND_THE_SCENES) çevirir, aksi halde serbest etiketi
     * yazıldığı gibi döner. Boşsa null.
     */
    public static String normalize(String raw) {
        if (raw == null) {
            return null;
        }
        String cleaned = raw.trim().replaceAll("\s+", " ");
        if (cleaned.isEmpty()) {
            return null;
        }
        String key = cleaned.toUpperCase(Locale.ROOT).replace(' ', '_').replace('-', '_');
        for (TriviaTag tag : values()) {
            if (tag.name().equals(key)) {
                return tag.name();
            }
        }
        return cleaned;
    }
}
