package com.example.fandoom_backend.lore.dto;

import com.example.fandoom_backend.lore.entity.TaggableType;

public record GroupAssignmentResponse(
        Long id, Long groupId, String groupName, String groupSlug,
        TaggableType taggableType, Long taggableId) {
}
