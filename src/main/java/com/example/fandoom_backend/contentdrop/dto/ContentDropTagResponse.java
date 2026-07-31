package com.example.fandoom_backend.contentdrop.dto;

import com.example.fandoom_backend.contentdrop.entity.SubjectType;

public record ContentDropTagResponse(
        Long id, SubjectType subjectType, Long subjectId,
        Integer seasonNumber, Integer episodeNumber, Long franchiseId) {
}
