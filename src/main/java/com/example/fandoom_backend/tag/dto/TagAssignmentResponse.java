package com.example.fandoom_backend.tag.dto;

import com.example.fandoom_backend.tag.entity.TagType;
import com.example.fandoom_backend.tag.entity.TaggableType;

public record TagAssignmentResponse(
        Long id, Long tagId, String tagName, String tagSlug, TagType tagType,
        TaggableType taggableType, Long taggableId) {}
