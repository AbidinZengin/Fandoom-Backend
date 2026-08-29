package com.example.fandoom_backend.account.dto;

import java.time.LocalDateTime;

// commentCount/theoryCount: community/ modülü kurulana kadar hep 0 döner
// (DTO kontratı baştan doğru, veri kaynağı sonradan bağlanacak).
public record ProfileStats(
        long commentCount,
        long theoryCount,
        long likeCount,
        long readBlogCount,
        LocalDateTime memberSince) {
}
