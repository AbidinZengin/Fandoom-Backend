package com.example.fandoom_backend.contentdrop.dto;

import com.example.fandoom_backend.contentdrop.entity.ContentDropStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ContentDropRequest(
        @NotBlank @Size(max = 255) String title,
        @Size(max = 255) String kicker,
        @Size(max = 255) String axis,
        @Size(max = 500) String imageUrl,
        @Size(max = 500) String imageUrlLarge,
        @Size(max = 255) String imageAlt,
        Integer spoilerThroughSeasonNumber,
        Integer spoilerThroughEpisodeNumber,
        @NotNull ContentDropStatus status,
        List<@Valid ContentDropBlockRequest> blocks,
        List<@Valid ContentDropTagRequest> tags) {
}
