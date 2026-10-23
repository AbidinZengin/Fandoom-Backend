package com.example.fandoom_backend.video.dto;

public record VideoResponse(
        Long id,
        String youtubeId,
        String title,
        String channelName,
        String channelUrl,
        int sortOrder) {
}
