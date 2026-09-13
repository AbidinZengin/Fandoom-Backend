package com.example.fandoom_backend.community.service;

import com.example.fandoom_backend.account.entity.ActivityType;
import com.example.fandoom_backend.account.entity.SavedItemType;
import com.example.fandoom_backend.account.service.ActivityLogService;
import com.example.fandoom_backend.account.service.UserFollowService;
import com.example.fandoom_backend.account.service.UserLikeService;
import com.example.fandoom_backend.community.dto.ProfileStats;
import com.example.fandoom_backend.community.dto.UpdateUserProfileRequest;
import com.example.fandoom_backend.community.dto.UserProfileResponse;
import com.example.fandoom_backend.community.entity.CommentStatus;
import com.example.fandoom_backend.community.entity.ThreadStatus;
import com.example.fandoom_backend.community.entity.ThreadSurface;
import com.example.fandoom_backend.community.entity.UserProfile;
import com.example.fandoom_backend.community.mapper.UserProfileMapper;
import com.example.fandoom_backend.community.repository.CommentRepository;
import com.example.fandoom_backend.community.repository.ThreadRepository;
import com.example.fandoom_backend.community.repository.UserProfileRepository;
import com.example.fandoom_backend.media.service.ImageStorageService;
import com.example.fandoom_backend.user.dto.UserDetailResponse;
import com.example.fandoom_backend.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

// UserLikeService/ActivityLogService/UserFollowService: account/'a cross-module
// erisim, interface uzerinden (dogrudan UserLikeRepository/UserActivityLogRepository/
// UserFollowRepository DEGIL — bkz. CLAUDE.md bagimsizlik kurallari). Dongu riski
// YOK: UserFollowServiceImpl yalnizca UserFollowRepository+ItemReferenceValidator'a
// bagimli, ItemReferenceValidator da Movie/Series/BlogService + (kisi takibi icin
// eklenen) user/UserService'e bagimli — hicbiri community/'ye dokunmuyor, tek
// yonlu community/ -> account/ zinciri bozulmuyor (bkz. CLAUDE.md Community
// Modulu notu). ThreadRepository/CommentRepository ise artik AYNI MODUL ICI
// (community/ tasindiktan sonra) — bilerek ThreadService/CommentService
// INTERFACE'LERI DEGIL doğrudan repository kullanılıyor: aksi halde
// ThreadServiceImpl/CommentServiceImpl'in avatarUrl icin bu servise
// (UserProfileService) bagimli olmasiyla BIRLIKTE gercek bir Spring circular
// bean dependency olusurdu (UserProfileServiceImpl -> ThreadService ->
// UserProfileService -> ...). Repository'ye inmek bu dongueyu kirar.
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserProfileServiceImpl implements UserProfileService {

    private final UserProfileRepository userProfileRepository;
    private final UserLikeService userLikeService;
    private final ActivityLogService activityLogService;
    private final UserProfileMapper userProfileMapper;
    private final UserService userService;
    private final ImageStorageService imageStorageService;
    private final ThreadRepository threadRepository;
    private final CommentRepository commentRepository;
    private final UserFollowService userFollowService;

    @Override
    public UserProfileResponse getProfile(Long userId, Long viewerId) {
        // Satır hiç yoksa (henüz PATCH edilmemiş) DB'ye yazılmadan, geçici/
        // bos varsayılan degerlerle bir UserProfile olusturulup mapper'a
        // aynen verilir — tasarım dokümanındaki "lazy, ilk PATCH'te olusur"
        // kuralı yalnızca YAZMA icin gecerli, GET hicbir zaman satir yaratmaz.
        UserProfile profile = userProfileRepository.findByUserId(userId)
                .orElseGet(() -> UserProfile.builder()
                        .userId(userId)
                        .bio("")
                        .spoilerProtectionEnabled(false)
                        .build());
        UserDetailResponse user = userService.getById(userId);
        long followerCount = userFollowService.count(SavedItemType.USER, userId);
        long followingCount = userFollowService.countFollowing(userId, SavedItemType.USER);
        boolean isFollowing = viewerId != null
                && userFollowService.getStatus(viewerId, SavedItemType.USER, userId).following();
        return userProfileMapper.toResponse(profile, userId, user.username(), buildStats(userId, user),
                followerCount, followingCount, isFollowing);
    }

    @Override
    @Transactional
    public UserProfileResponse updateProfile(Long userId, UpdateUserProfileRequest request) {
        UserProfile profile = userProfileRepository.findByUserId(userId)
                .orElseGet(() -> UserProfile.builder()
                        .userId(userId)
                        .bio("")
                        .spoilerProtectionEnabled(false)
                        .build());
        if (request.avatarUrl() != null) {
            imageStorageService.deleteIfChanged(profile.getAvatarUrl(), request.avatarUrl());
            profile.setAvatarUrl(request.avatarUrl());
        }
        if (request.bannerUrl() != null) {
            imageStorageService.deleteIfChanged(profile.getBannerUrl(), request.bannerUrl());
            profile.setBannerUrl(request.bannerUrl());
        }
        if (request.bio() != null) {
            profile.setBio(request.bio());
        }
        if (request.accentColor() != null) {
            profile.setAccentColor(request.accentColor());
        }
        if (request.spoilerProtectionEnabled() != null) {
            profile.setSpoilerProtectionEnabled(request.spoilerProtectionEnabled());
        }
        profile = userProfileRepository.save(profile);
        UserDetailResponse user = userService.getById(userId);
        long followerCount = userFollowService.count(SavedItemType.USER, userId);
        long followingCount = userFollowService.countFollowing(userId, SavedItemType.USER);
        // Kendi profilini güncelleyen kullanıcı kendini takip edemez (bkz.
        // UserFollowServiceImpl.follow self-follow guard) — isFollowing hep false.
        return userProfileMapper.toResponse(profile, userId, user.username(), buildStats(userId, user),
                followerCount, followingCount, false);
    }

    private ProfileStats buildStats(Long userId, UserDetailResponse user) {
        long likeCount = userLikeService.countByUserId(userId);
        long readBlogCount = activityLogService.countByUserIdAndType(userId, ActivityType.READ_BLOG);
        long commentCount = commentRepository.countByAuthorIdAndStatus(userId, CommentStatus.PUBLISHED);
        long theoryCount = threadRepository.countByAuthorIdAndSurfaceAndStatus(
                userId, ThreadSurface.THEORY, ThreadStatus.PUBLISHED);
        return new ProfileStats(commentCount, theoryCount, likeCount, readBlogCount, user.createdAt());
    }

    @Override
    public Map<Long, String> getAvatarUrlsByUserIds(Set<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        return userProfileRepository.findByUserIdIn(userIds).stream()
                .filter(p -> p.getAvatarUrl() != null)
                .collect(Collectors.toMap(UserProfile::getUserId, UserProfile::getAvatarUrl));
    }
}
