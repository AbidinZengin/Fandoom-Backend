package com.example.fandoom_backend.contentdrop.dto;

import com.example.fandoom_backend.contentdrop.entity.SubjectType;

// Ya (subjectType+subjectId, opsiyonel seasonNumber/episodeNumber) ya da
// yalnızca franchiseId doldurulur — ikisi birden değil. Servis katmanında
// (ContentDropServiceImpl.applyTags) doğrulanır.
public record ContentDropTagRequest(
        SubjectType subjectType,
        Long subjectId,
        Integer seasonNumber,
        Integer episodeNumber,
        Long franchiseId) {
}
