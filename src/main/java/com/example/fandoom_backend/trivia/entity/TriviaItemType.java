package com.example.fandoom_backend.trivia.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Locale;

// Trivia'nın bağlandığı yapım türü. API sözleşmesi gereği JSON'da/query'de küçük harf
// ("movie" | "series") görünür; DB'de enum adı (MOVIE/SERIES) saklanır.
public enum TriviaItemType {
    MOVIE,
    SERIES;

    @JsonValue
    public String toValue() {
        return name().toLowerCase(Locale.ROOT);
    }

    @JsonCreator
    public static TriviaItemType fromValue(String value) {
        if (value == null) {
            return null;
        }
        return valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}
