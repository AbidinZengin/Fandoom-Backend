package com.example.fandoom_backend.community.dto;

import com.example.fandoom_backend.community.entity.ThreadMediaType;

public record ThreadMediaResponse(ThreadMediaType type, String url, int position) {
}
