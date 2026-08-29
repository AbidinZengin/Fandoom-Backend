package com.example.fandoom_backend.account.dto;

import com.example.fandoom_backend.account.entity.SavedItemType;

import java.time.LocalDateTime;

public record UserSavedItemResponse(
        Long id,
        Long itemId,
        SavedItemType itemType,
        Integer progressPercentage,
        String notes,
        LocalDateTime createdAt) {
}
