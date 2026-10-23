package com.example.fandoom_backend.community.service;

import com.example.fandoom_backend.account.entity.ActivityType;
import com.example.fandoom_backend.account.entity.FollowState;
import com.example.fandoom_backend.account.service.ActivityLogService;
import com.example.fandoom_backend.account.service.ProfileFollowService;
import com.example.fandoom_backend.account.service.UserLikeService;
import com.example.fandoom_backend.community.dto.ProfileCard;
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
import com.example.fandoom_backend.common.exception.InvalidFileException;
import com.example.fandoom_backend.media.service.ImageStorageService;
import com.example.fandoom_backend.media.service.MediaUrlValidator;
import com.example.fandoom_backend.user.dto.UserDetailResponse;
import com.example.fandoom_backend.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

// UserLikeService/ActivityLogService/ProfileFollowService: account/'a cross-module
// erisim, interface uzerinden (dogrudan repository DEGIL — bkz. CLAUDE.md bagimsizlik
// kurallari). Dongu riski YOK: ProfileFollowServiceImpl yalnizca ProfileFollowRepository
// + user/UserService'e bagimli, community/'ye dokunmuyor (profil zenginlestirmesi
// account/FollowListService'te, o da bu servisi kullanir ama bu servis ona bagimli degil),
// tek yonlu community/ -> account/ zinciri bozulmuyor. ThreadRepository/CommentRepository ise artik AYNI MODUL ICI
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
    private final ProfileFollowService profileFollowService;
    private final MediaUrlValidator mediaUrlValidator;

    // Upload klasörleri (Cloudinary'de fandoom/<klasör>) ve boyut sınırları sunucuda sabittir.
    private static final String AVATAR_FOLDER = "avatars";
    private static final String BANNER_FOLDER = "banners";
    private static final long MAX_AVATAR_BYTES = 5L * 1024 * 1024;
    private static final long MAX_BANNER_BYTES = 8L * 1024 * 1024;

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
        long followerCount = profileFollowService.countFollowers(userId);
        long followingCount = profileFollowService.countFollowing(userId);
        boolean isSelf = viewerId != null && viewerId.equals(userId);
        FollowState state = isSelf ? FollowState.NONE : profileFollowService.stateOf(viewerId, userId);
        boolean restricted = profile.isPrivateProfile() && !isSelf && state != FollowState.FOLLOWING;
        // Kısıtlı izleyiciye içerik gösterilmeyeceği için istatistik sorguları hiç atılmaz.
        ProfileStats stats = restricted ? null : buildStats(userId, user);
        return userProfileMapper.toResponse(profile, userId, user.username(), stats,
                followerCount, followingCount, state == FollowState.FOLLOWING)
                .withViewer(isSelf, state, restricted);
    }

    // avatarUrl thread/yorum yazar özetlerine (AuthorSummary) gömülü: profil değişince thread cache'i bayatlamasın.
    @Override
    @Transactional
    @CacheEvict(cacheNames = {ThreadCacheNames.DETAIL, ThreadCacheNames.LIST}, allEntries = true)
    public UserProfileResponse updateProfile(Long userId, UpdateUserProfileRequest request) {
        return applyUpdate(userId, request);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = {ThreadCacheNames.DETAIL, ThreadCacheNames.LIST}, allEntries = true)
    public UserProfileResponse uploadAvatar(Long userId, MultipartFile file) {
        return uploadImage(userId, file, AVATAR_FOLDER, MAX_AVATAR_BYTES, "Avatar en fazla 5 MB olabilir",
                url -> new UpdateUserProfileRequest(null, url, null, null, null, null));
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = {ThreadCacheNames.DETAIL, ThreadCacheNames.LIST}, allEntries = true)
    public UserProfileResponse uploadBanner(Long userId, MultipartFile file) {
        return uploadImage(userId, file, BANNER_FOLDER, MAX_BANNER_BYTES, "Banner en fazla 8 MB olabilir",
                url -> new UpdateUserProfileRequest(null, null, url, null, null, null));
    }

    // Klasör SUNUCUDA sabit (istemci seçemez); boyut sınırı genel 10 MB'tan sıkı; tür/boş dosya kontrolünü
    // ImageStorageService yapar. DB yazımı başarısız olursa yeni yüklenen görsel yetim kalmasın diye silinir.
    private UserProfileResponse uploadImage(Long userId, MultipartFile file, String folder, long maxBytes,
                                            String tooLargeMessage,
                                            Function<String, UpdateUserProfileRequest> requestFor) {
        if (file != null && file.getSize() > maxBytes) {
            throw new InvalidFileException(tooLargeMessage);
        }
        String url = imageStorageService.upload(file, folder).url();
        try {
            return applyUpdate(userId, requestFor.apply(url));
        } catch (RuntimeException e) {
            try {
                imageStorageService.delete(url);
            } catch (RuntimeException cleanupFailure) {
                e.addSuppressed(cleanupFailure);
            }
            throw e;
        }
    }

    private UserProfileResponse applyUpdate(Long userId, UpdateUserProfileRequest request) {
        UserProfile profile = userProfileRepository.findByUserId(userId)
                .orElseGet(() -> UserProfile.builder()
                        .userId(userId)
                        .bio("")
                        .spoilerProtectionEnabled(false)
                        .build());
        if (request.avatarUrl() != null) {
            deleteReplacedImage(profile.getAvatarUrl(), request.avatarUrl(), AVATAR_FOLDER);
            profile.setAvatarUrl(request.avatarUrl());
        }
        if (request.bannerUrl() != null) {
            deleteReplacedImage(profile.getBannerUrl(), request.bannerUrl(), BANNER_FOLDER);
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
        boolean opensProfile = false;
        if (request.isPrivate() != null) {
            opensProfile = profile.isPrivateProfile() && !request.isPrivate();
            profile.setPrivateProfile(request.isPrivate());
        }
        profile = userProfileRepository.save(profile);
        // Gizlilik kapanınca bekleyen istekler otomatik onaylanır (aynı transaction).
        if (opensProfile) {
            profileFollowService.approveAllRequests(userId);
        }
        UserDetailResponse user = userService.getById(userId);
        long followerCount = profileFollowService.countFollowers(userId);
        long followingCount = profileFollowService.countFollowing(userId);
        // Kendi profilini güncelleyen kullanıcı kendini takip edemez (bkz.
        // ProfileFollowServiceImpl.follow self-follow guard) — isFollowing hep false.
        return userProfileMapper.toResponse(profile, userId, user.username(), buildStats(userId, user),
                followerCount, followingCount, false)
                .withViewer(true, FollowState.NONE, false);
    }

    // Eski görsel YALNIZCA sunucunun kendi klasöründeyse (fandoom/avatars|banners) silinir. avatarUrl/bannerUrl PATCH'te
    // kullanıcının verdiği serbest metindir: eski değer bir film posterinin/başkasının görselinin URL'siyse, "önce o URL'yi
    // yaz, sonra başka bir şeyle değiştir" hilesiyle onu Cloudinary'den sildirebilirdi (thread medyasındaki kuralla aynı).
    private void deleteReplacedImage(String oldUrl, String newUrl, String folder) {
        if (StringUtils.hasText(oldUrl) && !oldUrl.equals(newUrl) && mediaUrlValidator.isInFolder(oldUrl, folder)) {
            imageStorageService.delete(oldUrl);
        }
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

    @Override
    public Map<Long, ProfileCard> getCardsByUserIds(Set<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        return userProfileRepository.findByUserIdIn(userIds).stream()
                .collect(Collectors.toMap(UserProfile::getUserId,
                        p -> new ProfileCard(p.getAvatarUrl(), p.getBio(), p.getAccentColor(),
                                p.isPrivateProfile())));
    }
}
