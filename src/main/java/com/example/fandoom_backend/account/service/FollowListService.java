package com.example.fandoom_backend.account.service;

import com.example.fandoom_backend.account.dto.FollowUserResponse;
import com.example.fandoom_backend.common.dto.PageResponse;
import org.springframework.data.domain.Pageable;

// Takipçi/takip edilen listeleri: ProfileFollowService'ten sayfayı alır, username (user/) ve
// avatar/bio/accent (community/UserProfile) ile zenginleştirir. Ayrı servis çünkü
// ProfileFollowService community/'ye bağlanırsa döngü doğar.
public interface FollowListService {
    // viewerId nullable (anonim → following=false).
    PageResponse<FollowUserResponse> followers(String username, Long viewerId, Pageable pageable);
    PageResponse<FollowUserResponse> following(String username, Long viewerId, Pageable pageable);
}
