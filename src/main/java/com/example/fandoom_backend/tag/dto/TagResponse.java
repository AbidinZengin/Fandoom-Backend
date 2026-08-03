package com.example.fandoom_backend.tag.dto;

import com.example.fandoom_backend.tag.entity.TagType;

public record TagResponse(Long id, String name, String slug, String description, TagType type) {}
