package com.example.fandoom_backend.community.dto;

import com.example.fandoom_backend.community.entity.CommentSubjectType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// Merkezi POST /api/community/comments ucu için — subjectType/subjectId
// body'de taşınır (GET tarafında query param olarak, tutarlılık için ayrı bir
// DTO). CommentRequest'in geri kalanıyla aynı validasyon kuralları.
public record CreateCommentRequest(
        @NotNull CommentSubjectType subjectType,
        @NotNull Long subjectId,
        @NotBlank @Size(min = 2, max = 2000) String body,
        boolean spoilerFlagged,
        Long parentId) {

    public CommentRequest toCommentRequest() {
        return new CommentRequest(body, spoilerFlagged, parentId);
    }
}
