package com.example.fandoom_backend.account.service;

import com.example.fandoom_backend.account.dto.ProfileStats;
import com.example.fandoom_backend.account.dto.UpdateUserProfileRequest;
import com.example.fandoom_backend.account.dto.UserProfileResponse;
import com.example.fandoom_backend.account.entity.ActivityType;
import com.example.fandoom_backend.account.entity.UserProfile;
import com.example.fandoom_backend.account.mapper.UserProfileMapper;
import com.example.fandoom_backend.account.repository.UserActivityLogRepository;
import com.example.fandoom_backend.account.repository.UserLikeRepository;
import com.example.fandoom_backend.account.repository.UserProfileRepository;
import com.example.fandoom_backend.community.entity.ThreadSurface;
import com.example.fandoom_backend.community.service.CommentService;
import com.example.fandoom_backend.community.service.ThreadService;
import com.example.fandoom_backend.media.service.ImageStorageService;
import com.example.fandoom_backend.user.dto.UserDetailResponse;
import com.example.fandoom_backend.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserProfileServiceImpl implements UserProfileService {

    private final UserProfileRepository userProfileRepository;
    private final UserLikeRepository userLikeRepository;
    private final UserActivityLogRepository userActivityLogRepository;
    private final UserProfileMapper userProfileMapper;
    private final UserService userService;
    private final ImageStorageService imageStorageService;
    private final ThreadService threadService;
    private final CommentService commentService;

    @Override
    public UserProfileResponse getProfile(Long userId) {
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
        return userProfileMapper.toResponse(profile, user.username(), buildStats(userId, user));
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
        return userProfileMapper.toResponse(profile, user.username(), buildStats(userId, user));
    }

    private ProfileStats buildStats(Long userId, UserDetailResponse user) {
        long likeCount = userLikeRepository.countByUserId(userId);
        long readBlogCount = userActivityLogRepository.countByUserIdAndActivityType(userId, ActivityType.READ_BLOG);
        long commentCount = commentService.countByAuthorId(userId);
        long theoryCount = threadService.countByAuthorIdAndSurface(userId, ThreadSurface.THEORY);
        return new ProfileStats(commentCount, theoryCount, likeCount, readBlogCount, user.createdAt());
    }
}
