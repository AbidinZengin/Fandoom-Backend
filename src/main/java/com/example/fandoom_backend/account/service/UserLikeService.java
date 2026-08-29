package com.example.fandoom_backend.account.service;

import com.example.fandoom_backend.account.dto.LikeStatusResponse;
import com.example.fandoom_backend.account.dto.UserLikeResponse;
import com.example.fandoom_backend.account.entity.SavedItemType;
import com.example.fandoom_backend.common.dto.PageResponse;
import org.springframework.data.domain.Pageable;

public interface UserLikeService {
    // Idempotent — zaten beğenilmişse hata vermez, mevcut durumu döner.
    LikeStatusResponse like(Long userId, SavedItemType itemType, Long itemId);
    // Idempotent — beğenilmemiş olduğunda no-op.
    LikeStatusResponse unlike(Long userId, SavedItemType itemType, Long itemId);
    LikeStatusResponse getStatus(Long userId, SavedItemType itemType, Long itemId);
    PageResponse<UserLikeResponse> list(Long userId, Pageable pageable);
    long count(SavedItemType itemType, Long itemId);
}
