package com.example.fandoom_backend.group.dto;

import com.example.fandoom_backend.group.entity.GroupType;
import com.example.fandoom_backend.group.entity.SubjectType;

public record GroupResponse(
        Long id,
        String name,
        String slug,
        String imageUrl,
        GroupType type,
        SubjectType subjectType,
        Long subjectId) {
}
