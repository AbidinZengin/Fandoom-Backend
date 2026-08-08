package com.example.fandoom_backend.group.dto;

import com.example.fandoom_backend.group.entity.GroupType;
import com.example.fandoom_backend.group.entity.TaggableType;

public record GroupAssignmentResponse(
        Long id, Long groupId, String groupName, String groupSlug, GroupType groupType,
        TaggableType taggableType, Long taggableId) {
}
