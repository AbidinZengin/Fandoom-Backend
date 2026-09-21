package com.example.fandoom_backend.community.dto;

// Thread yanıtlarındaki `portal` alanı: name istek diline göre çözülmüş (yoksa TR<->EN fallback).
public record PortalRefResponse(String slug, String name) {
}
