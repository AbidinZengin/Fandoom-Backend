package com.example.fandoom_backend.trivia.dto;

import com.example.fandoom_backend.trivia.entity.TriviaItemType;
import com.example.fandoom_backend.trivia.entity.TriviaTag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record TriviaRequest(
        @NotNull Long itemId,
        @NotNull TriviaItemType itemType,
        @NotBlank @Size(max = 2000) String content,
        @NotNull TriviaTag tag,
        // null = false (atlanan alan default'a düşer)
        Boolean isSpoiler,
        @Size(max = 500) @Pattern(regexp = "^https?://\\S+$", message = "sourceUrl http(s) ile başlamalı") String sourceUrl) {
}
