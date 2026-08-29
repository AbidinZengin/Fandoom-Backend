package com.example.fandoom_backend.account.dto;

import com.example.fandoom_backend.account.entity.SavedItemType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// targetListId null ise itemType'a göre sistem listesi otomatik çözülür
// (BLOG->READLIST, MOVIE|SERIES->WATCHLIST) — bkz. UserSavedItemServiceImpl.
public record SaveItemRequest(
        @NotNull Long itemId,
        @NotNull SavedItemType itemType,
        Long targetListId,
        @Size(max = 500) String notes) {
}
