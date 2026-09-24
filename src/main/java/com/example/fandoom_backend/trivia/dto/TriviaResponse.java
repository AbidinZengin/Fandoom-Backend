package com.example.fandoom_backend.trivia.dto;

import com.example.fandoom_backend.trivia.entity.TriviaItemType;
import com.example.fandoom_backend.trivia.entity.TriviaTag;

import java.time.LocalDateTime;

public record TriviaResponse(
        Long id,
        Long itemId,
        TriviaItemType itemType,
        String content,
        TriviaTag tag,
        boolean isSpoiler,
        String sourceUrl,
        Long createdBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
