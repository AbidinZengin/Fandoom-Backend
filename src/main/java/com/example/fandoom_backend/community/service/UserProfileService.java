package com.example.fandoom_backend.community.service;

import com.example.fandoom_backend.community.dto.UpdateUserProfileRequest;
import com.example.fandoom_backend.community.dto.UserProfileResponse;

import java.util.Map;
import java.util.Set;

public interface UserProfileService {
    // viewerId nullable — anonim istek veya kendi profiline bakış (ThreadService.getBySlug
    // deseniyle aynı). isFollowing hesaplaması için kullanılır, updateProfile'ın
    // aksine burada "kim bakıyor" anlamlı (bkz. CLAUDE.md Community Modülü notu).
    UserProfileResponse getProfile(Long userId, Long viewerId);
    UserProfileResponse updateProfile(Long userId, UpdateUserProfileRequest request);
    // Thread/Comment yazar zenginleştirmesi (avatarUrl) için toplu çözüm —
    // bilinmeyen id'ler (hiç profil oluşturmamış kullanıcı) map'te yer almaz,
    // çağıran taraf Map.get ile null-güvenli okumalı (getUsernamesByIds deseniyle tutarlı).
    Map<Long, String> getAvatarUrlsByUserIds(Set<Long> userIds);
}
