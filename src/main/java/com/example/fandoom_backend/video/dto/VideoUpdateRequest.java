package com.example.fandoom_backend.video.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// youtubeId bilinçli olarak yok (değişmez). sortOrder boşsa mevcut değer korunur.
public record VideoUpdateRequest(
        @NotBlank @Size(max = 255) String title,
        @NotBlank @Size(max = 120) String channelName,
        @NotBlank @Size(max = 500) @Pattern(regexp = "^https://\\S+$", message = "channelUrl https:// ile başlamalı")
        String channelUrl,
        Integer sortOrder) {
}
