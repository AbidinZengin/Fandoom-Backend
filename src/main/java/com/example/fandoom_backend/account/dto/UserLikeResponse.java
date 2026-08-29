package com.example.fandoom_backend.account.dto;

import com.example.fandoom_backend.account.entity.SavedItemType;

import java.time.LocalDateTime;

// GET /api/me/likes listesi için: LikeStatusResponse (liked/likeCount) tekil
// bir hedefin durumunu anlatır, bu ise kullanıcının beğendiği HER öğeyi
// (itemId/itemType) satır satır döner — UserFollowResponse ile aynı desen.
public record UserLikeResponse(
        Long id,
        Long itemId,
        SavedItemType itemType,
        LocalDateTime createdAt) {
}
