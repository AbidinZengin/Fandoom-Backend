package com.example.fandoom_backend.account.dto;

import com.example.fandoom_backend.account.entity.ListType;
import com.example.fandoom_backend.common.dto.PageResponse;

public record UserListDetailResponse(
        Long id,
        String title,
        String description,
        String coverImageUrl,
        ListType listType,
        boolean isPublic,
        boolean isPinned,
        PageResponse<UserSavedItemResponse> items) {
}
