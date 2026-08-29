package com.example.fandoom_backend.account.service;

import com.example.fandoom_backend.account.dto.BookmarkStatusResponse;
import com.example.fandoom_backend.account.dto.UserBookmarkResponse;
import com.example.fandoom_backend.account.entity.SavedItemType;
import com.example.fandoom_backend.common.dto.PageResponse;
import org.springframework.data.domain.Pageable;

public interface UserBookmarkService {
    // Idempotent — zaten kaydedilmişse hata vermez, mevcut durumu döner.
    BookmarkStatusResponse bookmark(Long userId, SavedItemType itemType, Long itemId);
    // Idempotent — kaydedilmemiş olduğunda no-op.
    BookmarkStatusResponse unbookmark(Long userId, SavedItemType itemType, Long itemId);
    BookmarkStatusResponse getStatus(Long userId, SavedItemType itemType, Long itemId);
    PageResponse<UserBookmarkResponse> list(Long userId, Pageable pageable);
    long count(SavedItemType itemType, Long itemId);
}
