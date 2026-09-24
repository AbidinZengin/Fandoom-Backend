package com.example.fandoom_backend.trivia.dto;

import com.example.fandoom_backend.trivia.entity.TriviaItemType;

import java.time.LocalDateTime;
import java.util.List;

// tag = birincil (ilk) etiket, tags = tümü (hazır değerler ve serbest metin karışık).
// title/content istek diline göre çözülür (tr ise *Tr, boşsa EN'e düşer); titleTr/contentTr ham döner.
public record TriviaResponse(
        Long id,
        Long itemId,
        TriviaItemType itemType,
        String title,
        String titleTr,
        String content,
        String contentTr,
        String imageUrl,
        String tag,
        List<String> tags,
        boolean isSpoiler,
        String sourceUrl,
        Long createdBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
