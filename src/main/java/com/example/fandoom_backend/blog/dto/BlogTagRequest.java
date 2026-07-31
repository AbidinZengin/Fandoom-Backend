package com.example.fandoom_backend.blog.dto;

import com.example.fandoom_backend.blog.entity.SubjectType;
import jakarta.validation.constraints.Size;

// Ya (subjectType+subjectId, opsiyonel seasonNumber/episodeNumber) ya da
// yalnızca franchiseId ya da yalnızca category doldurulur — üçü birden
// değil. Servis katmanında (BlogServiceImpl.applyTags) doğrulanır.
public record BlogTagRequest(
        SubjectType subjectType,
        Long subjectId,
        Integer seasonNumber,
        Integer episodeNumber,
        Long franchiseId,
        @Size(max = 100) String category) {
}
