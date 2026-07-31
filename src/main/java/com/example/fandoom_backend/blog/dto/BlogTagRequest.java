package com.example.fandoom_backend.blog.dto;

import com.example.fandoom_backend.blog.entity.SubjectType;

// Ya (subjectType+subjectId, opsiyonel seasonNumber/episodeNumber) ya da
// yalnızca franchiseId doldurulur — ikisi birden değil. Servis katmanında
// (BlogServiceImpl.applyTags) doğrulanır.
public record BlogTagRequest(
        SubjectType subjectType,
        Long subjectId,
        Integer seasonNumber,
        Integer episodeNumber,
        Long franchiseId) {
}
