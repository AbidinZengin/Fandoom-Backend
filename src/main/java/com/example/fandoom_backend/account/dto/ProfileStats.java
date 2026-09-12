package com.example.fandoom_backend.account.dto;

import java.time.LocalDateTime;

// commentCount: community/'deki CommentService.countByAuthorId'den (PUBLISHED
// yorumlar). theoryCount: ThreadService.countByAuthorIdAndSurface(THEORY)'den
// (PUBLISHED tema thread'leri). Bkz. UserProfileServiceImpl.buildStats.
public record ProfileStats(
        long commentCount,
        long theoryCount,
        long likeCount,
        long readBlogCount,
        LocalDateTime memberSince) {
}
