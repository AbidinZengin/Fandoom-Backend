package com.example.fandoom_backend.account.dto;

import com.example.fandoom_backend.account.entity.SavedItemType;

import java.time.LocalDateTime;

// GET /api/me/follows listesi için: FollowStatusResponse (following/
// followerCount) tekil bir hedefin durumunu anlatır, bu ise kullanıcının
// takip ettiği HER öğeyi (itemId/itemType) satır satır döner.
public record UserFollowResponse(
        Long id,
        Long itemId,
        SavedItemType itemType,
        LocalDateTime createdAt) {
}
