package com.example.fandoom_backend.video.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// sortOrder boşsa video listenin sonuna eklenir.
public record VideoCreateRequest(
        @NotBlank @Pattern(regexp = "^[A-Za-z0-9_-]{11}$", message = "youtubeId 11 karakterlik geçerli bir YouTube id olmalı")
        String youtubeId,
        @NotBlank @Size(max = 255) String title,
        @NotBlank @Size(max = 120) String channelName,
        @NotBlank @Size(max = 500) @Pattern(regexp = "^https://\\S+$", message = "channelUrl https:// ile başlamalı")
        String channelUrl,
        Integer sortOrder) {
}
