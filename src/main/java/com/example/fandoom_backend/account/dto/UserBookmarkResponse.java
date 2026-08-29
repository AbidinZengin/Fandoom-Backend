package com.example.fandoom_backend.account.dto;

import com.example.fandoom_backend.account.entity.SavedItemType;

import java.time.LocalDateTime;

// GET /api/me/bookmarks listesi için: BookmarkStatusResponse (bookmarked/
// bookmarkCount) tekil bir hedefin durumunu anlatır, bu ise kullanıcının
// kaydettiği HER öğeyi (itemId/itemType) satır satır döner — UserLikeResponse/
// UserFollowResponse ile aynı desen.
public record UserBookmarkResponse(
        Long id,
        Long itemId,
        SavedItemType itemType,
        LocalDateTime createdAt) {
}
