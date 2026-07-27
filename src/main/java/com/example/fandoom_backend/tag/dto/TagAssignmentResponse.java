package com.example.fandoom_backend.tag.dto;

import com.example.fandoom_backend.tag.entity.TaggableType;

public record TagAssignmentResponse(
        Long id, Long tagId, String tagName, String tagSlug,
        TaggableType taggableType, Long taggableId) {}
