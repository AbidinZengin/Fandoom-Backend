package com.example.fandoom_backend.blog.dto;

import com.example.fandoom_backend.blog.entity.BlogStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record BlogRequest(
        @NotBlank @Size(max = 255) String title,
        @Size(max = 255) String kicker,
        @Size(max = 255) String axis,
        @Size(max = 500) String imageUrl,
        @Size(max = 500) String imageUrlLarge,
        @Size(max = 255) String imageAlt,
        Integer spoilerThroughSeasonNumber,
        Integer spoilerThroughEpisodeNumber,
        @NotNull BlogStatus status,
        List<@Valid BlogBlockRequest> blocks,
        List<@Valid BlogTagRequest> tags) {
}
