package com.example.fandoom_backend.account.service;

import com.example.fandoom_backend.account.dto.FollowStatusResponse;
import com.example.fandoom_backend.account.dto.UserFollowResponse;
import com.example.fandoom_backend.account.entity.SavedItemType;
import com.example.fandoom_backend.common.dto.PageResponse;
import org.springframework.data.domain.Pageable;

public interface UserFollowService {
    // Idempotent — zaten takip ediliyorsa hata vermez, mevcut durumu döner.
    FollowStatusResponse follow(Long userId, SavedItemType itemType, Long itemId);
    // Idempotent — takip edilmiyor olduğunda no-op.
    FollowStatusResponse unfollow(Long userId, SavedItemType itemType, Long itemId);
    PageResponse<UserFollowResponse> list(Long userId, Pageable pageable);
    long count(SavedItemType itemType, Long itemId);
}
