package com.example.fandoom_backend.community.dto;

import java.time.LocalDateTime;
import java.util.List;

// status==DELETED ise body mapper'da "[silindi]" olarak maskelenir (orijinal
// veri DB'de korunur, bkz. Comment entity yorumu). replies: yalnızca üst
// seviye yorumlarda dolu (max 3 önizleme); bir yanıtın kendi replies'ı hep [].
public record CommentResponse(
        Long id, Long threadId, Long parentId, String body,
        boolean spoilerFlagged, Long authorId, AuthorSummary author, int likeCount,
        boolean isLiked, int replyCount, List<CommentResponse> replies,
        LocalDateTime createdAt) {
}
