package com.example.fandoom_backend.trivia.dto;

import com.example.fandoom_backend.trivia.entity.TriviaItemType;
import com.example.fandoom_backend.trivia.entity.TriviaTag;

import java.time.LocalDateTime;

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
        TriviaTag tag,
        boolean isSpoiler,
        String sourceUrl,
        Long createdBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
