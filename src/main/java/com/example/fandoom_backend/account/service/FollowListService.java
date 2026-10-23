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
    // Kullanıcı dizini (keşfet / arama): e-postası doğrulanmış kullanıcılar, en yeni üye önce. query null/boş ise
    // hepsi, doluysa kullanıcı adında geçenler (büyük/küçük harf duyarsız). Sayfa boyutu en fazla 50.
    PageResponse<FollowUserResponse> users(String query, Long viewerId, Pageable pageable);
    // Gizli profil sahibinin bekleyen takip istekleri (kendi hesabı için), en yeni önce.
    PageResponse<FollowUserResponse> pendingRequests(Long userId, Pageable pageable);
}
