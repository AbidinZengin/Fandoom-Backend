package com.example.fandoom_backend.account.dto;

import com.example.fandoom_backend.account.entity.ListType;

public record UserListSummaryResponse(
        Long id,
        String title,
        String coverImageUrl,
        ListType listType,
        long itemCount,
        boolean isPublic,
        boolean isPinned) {
}
