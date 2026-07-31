package com.example.fandoom_backend.blog.dto;

import com.example.fandoom_backend.blog.entity.SubjectType;

public record BlogTagResponse(
        Long id, SubjectType subjectType, Long subjectId,
        Integer seasonNumber, Integer episodeNumber, Long franchiseId, String category) {
}
