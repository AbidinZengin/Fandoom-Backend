package com.example.fandoom_backend.community.service;

import com.example.fandoom_backend.community.dto.ProfileCard;
import com.example.fandoom_backend.community.dto.UpdateUserProfileRequest;
import com.example.fandoom_backend.community.dto.UserProfileResponse;

import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.Set;

public interface UserProfileService {
    // viewerId nullable — anonim istek veya kendi profiline bakış (ThreadService.getBySlug
    // deseniyle aynı). isFollowing hesaplaması için kullanılır, updateProfile'ın
    // aksine burada "kim bakıyor" anlamlı (bkz. CLAUDE.md Community Modülü notu).
    UserProfileResponse getProfile(Long userId, Long viewerId);
    UserProfileResponse updateProfile(Long userId, UpdateUserProfileRequest request);
    // Dosyayı yükler (klasör sunucuda sabit: avatars/banners), profile yazar, eski görseli siler, güncel profili döner.
    // Avatar en fazla 5 MB, banner en fazla 8 MB; tür/boş dosya hatası InvalidFileException (400).
    UserProfileResponse uploadAvatar(Long userId, MultipartFile file);
    UserProfileResponse uploadBanner(Long userId, MultipartFile file);
    // Thread/Comment yazar zenginleştirmesi (avatarUrl) için toplu çözüm —
    // bilinmeyen id'ler (hiç profil oluşturmamış kullanıcı) map'te yer almaz,
    // çağıran taraf Map.get ile null-güvenli okumalı (getUsernamesByIds deseniyle tutarlı).
    Map<Long, String> getAvatarUrlsByUserIds(Set<Long> userIds);
    // Takipçi/takip listeleri için toplu profil özeti; profil satırı olmayan kullanıcı map'te yer almaz.
    Map<Long, ProfileCard> getCardsByUserIds(Set<Long> userIds);
}
