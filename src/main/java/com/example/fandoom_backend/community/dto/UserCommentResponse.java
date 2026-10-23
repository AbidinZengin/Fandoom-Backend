package com.example.fandoom_backend.community.dto;

import com.example.fandoom_backend.community.entity.CommentSubjectType;
import com.example.fandoom_backend.community.entity.ThreadSurface;

import java.time.LocalDateTime;

// Profildeki "Yorumlar" sekmesi satırı. THREAD yorumlarında threadSlug/threadTitle/surface dolu;
// BLOG/SEASON/EPISODE yorumlarında bunlar null, FE subjectType+subjectId ile link kurar.
public record UserCommentResponse(
        Long id, String body, boolean spoilerFlagged, LocalDateTime createdAt,
        CommentSubjectType subjectType, Long subjectId,
        String threadSlug, String threadTitle, ThreadSurface surface) {
}
