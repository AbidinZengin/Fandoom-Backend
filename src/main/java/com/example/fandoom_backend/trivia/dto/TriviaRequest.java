package com.example.fandoom_backend.trivia.dto;

import com.example.fandoom_backend.trivia.entity.TriviaItemType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public record TriviaRequest(
        @NotNull Long itemId,
        @NotNull TriviaItemType itemType,
        @Size(max = 255) String title,
        @Size(max = 255) String titleTr,
        @NotBlank @Size(max = 2000) String content,
        @Size(max = 2000) String contentTr,
        @Size(max = 500) String imageUrl,
        // Etiketler: hazır (TriviaTag) ya da serbest metin. `tag` = spec'teki tek etiket (geriye uyumluluk),
        // `tags` ile birleştirilir; en az biri zorunlu, toplam en fazla 5.
        @Size(max = 30) @Pattern(regexp = "^[^<>]*$", message = "tag < veya > içeremez") String tag,
        @Size(max = 5) List<@Size(max = 30) @Pattern(regexp = "^[^<>]*$", message = "tag < veya > içeremez") String> tags,
        // null = false (atlanan alan default'a düşer)
        Boolean isSpoiler,
        @Size(max = 500) @Pattern(regexp = "^https?://\\S+$", message = "sourceUrl http(s) ile başlamalı") String sourceUrl) {
}
