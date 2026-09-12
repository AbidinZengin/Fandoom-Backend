package com.example.fandoom_backend.community.dto;

import com.example.fandoom_backend.community.entity.CommentSubjectType;

import java.time.LocalDateTime;
import java.util.List;

// status==DELETED ise body mapper'da "[silindi]" olarak maskelenir (orijinal
// veri DB'de korunur, bkz. Comment entity yorumu). replies: yalnızca üst
// seviye yorumlarda dolu (max 3 önizleme); bir yanıtın kendi replies'ı hep [].
// threadId: geriye uyumluluk için korunur, subjectType==THREAD iken
// subjectId'yle aynı değeri taşır, diğer subjectType'larda null döner —
// yeni entegrasyonlar subjectType/subjectId'yi kullanmalı.
public record CommentResponse(
        Long id, CommentSubjectType subjectType, Long subjectId, Long threadId, Long parentId, String body,
        boolean spoilerFlagged, Long authorId, AuthorSummary author, int likeCount,
        boolean isLiked, int replyCount, List<CommentResponse> replies,
        LocalDateTime createdAt) {
}
