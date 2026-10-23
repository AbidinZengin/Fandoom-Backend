package com.example.fandoom_backend.community.service;

import com.example.fandoom_backend.account.entity.ActivityType;
import com.example.fandoom_backend.account.entity.FollowState;
import com.example.fandoom_backend.account.service.ActivityLogService;
import com.example.fandoom_backend.account.service.ProfileFollowService;
import com.example.fandoom_backend.account.service.UserLikeService;
import com.example.fandoom_backend.community.dto.ProfileStats;
import com.example.fandoom_backend.community.dto.UpdateUserProfileRequest;
import com.example.fandoom_backend.community.dto.UserProfileResponse;
import com.example.fandoom_backend.community.entity.ThreadSurface;
import com.example.fandoom_backend.community.entity.UserProfile;
import com.example.fandoom_backend.community.entity.CommentStatus;
import com.example.fandoom_backend.community.entity.ThreadStatus;
import com.example.fandoom_backend.community.mapper.UserProfileMapper;
import com.example.fandoom_backend.community.repository.CommentRepository;
import com.example.fandoom_backend.community.repository.ThreadRepository;
import com.example.fandoom_backend.community.repository.UserProfileRepository;
import com.example.fandoom_backend.common.exception.InvalidFileException;
import com.example.fandoom_backend.media.dto.MediaUploadResponse;
import com.example.fandoom_backend.media.service.ImageStorageService;
import com.example.fandoom_backend.media.service.MediaUrlValidator;
import com.example.fandoom_backend.user.dto.UserDetailResponse;
import com.example.fandoom_backend.user.entity.Role;
import com.example.fandoom_backend.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceImplTest {

    private static final Long USER_ID = 7L;

    @Mock
    private UserProfileRepository userProfileRepository;
    @Mock
    private UserLikeService userLikeService;
    @Mock
    private ActivityLogService activityLogService;
    @Mock
    private UserProfileMapper userProfileMapper;
    @Mock
    private UserService userService;
    @Mock
    private ImageStorageService imageStorageService;
    @Mock
    private ThreadRepository threadRepository;
    @Mock
    private CommentRepository commentRepository;
    @Mock
    private ProfileFollowService profileFollowService;
    @Mock
    private MediaUrlValidator mediaUrlValidator;

    private UserProfileServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new UserProfileServiceImpl(userProfileRepository, userLikeService, activityLogService,
                userProfileMapper, userService, imageStorageService, threadRepository, commentRepository,
                profileFollowService, mediaUrlValidator);

        lenient().when(userService.getById(USER_ID)).thenReturn(new UserDetailResponse(
                USER_ID, "abidin", "abidin@example.com", Role.USER, true, null, false,
                LocalDateTime.of(2026, 1, 1, 0, 0)));
        lenient().when(userProfileMapper.toResponse(any(), any(), any(), any(), anyLong(), anyLong(), anyBoolean()))
                .thenReturn(new UserProfileResponse(USER_ID, "abidin", "", null, null, null, false,
                        new ProfileStats(0, 0, 0, 0, null), 0L, 0L, false, false, false, false, false, null));
        lenient().when(profileFollowService.countFollowers(USER_ID)).thenReturn(0L);
        lenient().when(profileFollowService.countFollowing(USER_ID)).thenReturn(0L);
        lenient().when(profileFollowService.stateOf(any(), eq(USER_ID))).thenReturn(FollowState.NONE);
    }

    @Test
    void getProfile_noExistingRow_buildsTransientDefaultsWithoutPersisting() {
        when(userProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

        service.getProfile(USER_ID, null);

        ArgumentCaptor<UserProfile> captor = ArgumentCaptor.forClass(UserProfile.class);
        verify(userProfileMapper).toResponse(captor.capture(), eq(USER_ID), eq("abidin"), any(),
                anyLong(), anyLong(), anyBoolean());
        UserProfile passed = captor.getValue();
        assertThat(passed.getId()).isNull();
        assertThat(passed.getUserId()).isEqualTo(USER_ID);
        assertThat(passed.getBio()).isEmpty();
        assertThat(passed.isSpoilerProtectionEnabled()).isFalse();
        verify(userProfileRepository, never()).save(any());
    }

    @Test
    void getProfile_existingRow_usesStoredProfile() {
        UserProfile existing = UserProfile.builder().id(1L).userId(USER_ID).bio("merhaba")
                .accentColor("#ffffff").spoilerProtectionEnabled(true).build();
        when(userProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(existing));

        service.getProfile(USER_ID, null);

        verify(userProfileMapper).toResponse(eq(existing), eq(USER_ID), eq("abidin"), any(),
                anyLong(), anyLong(), anyBoolean());
    }

    @Test
    void getProfile_computesStatsFromLikeAndActivityRepositories() {
        when(userProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
        when(userLikeService.countByUserId(USER_ID)).thenReturn(3L);
        when(activityLogService.countByUserIdAndType(USER_ID, ActivityType.READ_BLOG)).thenReturn(5L);
        when(commentRepository.countByAuthorIdAndStatus(USER_ID, CommentStatus.PUBLISHED)).thenReturn(2L);
        when(threadRepository.countByAuthorIdAndSurfaceAndStatus(USER_ID, ThreadSurface.THEORY, ThreadStatus.PUBLISHED))
                .thenReturn(1L);

        service.getProfile(USER_ID, null);

        ArgumentCaptor<ProfileStats> statsCaptor = ArgumentCaptor.forClass(ProfileStats.class);
        verify(userProfileMapper).toResponse(any(), eq(USER_ID), eq("abidin"), statsCaptor.capture(),
                anyLong(), anyLong(), anyBoolean());
        ProfileStats stats = statsCaptor.getValue();
        assertThat(stats.likeCount()).isEqualTo(3L);
        assertThat(stats.readBlogCount()).isEqualTo(5L);
        assertThat(stats.commentCount()).isEqualTo(2L);
        assertThat(stats.theoryCount()).isEqualTo(1L);
        assertThat(stats.memberSince()).isEqualTo(LocalDateTime.of(2026, 1, 1, 0, 0));
    }

    @Test
    void getProfile_viewerIsNull_isFollowingIsFalse() {
        when(userProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

        service.getProfile(USER_ID, null);

        ArgumentCaptor<Boolean> isFollowingCaptor = ArgumentCaptor.forClass(Boolean.class);
        verify(userProfileMapper).toResponse(any(), eq(USER_ID), eq("abidin"), any(), anyLong(), anyLong(),
                isFollowingCaptor.capture());
        assertThat(isFollowingCaptor.getValue()).isFalse();
    }

    @Test
    void getProfile_viewerFollowsUser_isFollowingReflectsStatus() {
        Long viewerId = 99L;
        when(userProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
        when(profileFollowService.stateOf(viewerId, USER_ID)).thenReturn(FollowState.FOLLOWING);

        service.getProfile(USER_ID, viewerId);

        ArgumentCaptor<Boolean> isFollowingCaptor = ArgumentCaptor.forClass(Boolean.class);
        verify(userProfileMapper).toResponse(any(), eq(USER_ID), eq("abidin"), any(), anyLong(), anyLong(),
                isFollowingCaptor.capture());
        assertThat(isFollowingCaptor.getValue()).isTrue();
    }

    @Test
    void getProfile_computesFollowerAndFollowingCountsFromProfileFollowService() {
        when(userProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
        when(profileFollowService.countFollowers(USER_ID)).thenReturn(12L);
        when(profileFollowService.countFollowing(USER_ID)).thenReturn(4L);

        service.getProfile(USER_ID, null);

        verify(userProfileMapper).toResponse(any(), eq(USER_ID), eq("abidin"), any(), eq(12L), eq(4L), anyBoolean());
    }

    private UserProfile privateProfile() {
        return UserProfile.builder().id(1L).userId(USER_ID).bio("gizli").privateProfile(true).build();
    }

    private UserProfileResponse fullResponse() {
        return new UserProfileResponse(USER_ID, "abidin", "gizli", "a.png", "b.png", "#ffffff", true,
                new ProfileStats(1, 1, 1, 1, null), 5L, 6L, false, false, false, true, false, null);
    }

    @Test
    void getProfile_privateProfile_strangerIsRestrictedAndContentMasked() {
        Long viewerId = 99L;
        when(userProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(privateProfile()));
        when(profileFollowService.stateOf(viewerId, USER_ID)).thenReturn(FollowState.REQUESTED);
        when(userProfileMapper.toResponse(any(), any(), any(), any(), anyLong(), anyLong(), anyBoolean()))
                .thenReturn(fullResponse());

        UserProfileResponse result = service.getProfile(USER_ID, viewerId);

        assertThat(result.restricted()).isTrue();
        assertThat(result.followStatus()).isEqualTo(FollowState.REQUESTED);
        assertThat(result.bio()).isNull();
        assertThat(result.accentColor()).isNull();
        assertThat(result.stats()).isNull();
        assertThat(result.avatarUrl()).isEqualTo("a.png");
        assertThat(result.isPrivate()).isTrue();
        assertThat(result.followerCount()).isEqualTo(5L);
        // Kısıtlı izleyiciye istatistik hesaplanmaz.
        verify(userLikeService, never()).countByUserId(any());
    }

    @Test
    void getProfile_privateProfile_approvedFollowerSeesEverything() {
        Long viewerId = 99L;
        when(userProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(privateProfile()));
        when(profileFollowService.stateOf(viewerId, USER_ID)).thenReturn(FollowState.FOLLOWING);
        when(userProfileMapper.toResponse(any(), any(), any(), any(), anyLong(), anyLong(), anyBoolean()))
                .thenReturn(fullResponse());

        UserProfileResponse result = service.getProfile(USER_ID, viewerId);

        assertThat(result.restricted()).isFalse();
        assertThat(result.following()).isTrue();
        assertThat(result.bio()).isEqualTo("gizli");
    }

    @Test
    void getProfile_privateProfile_ownerIsSelfAndNotRestricted() {
        when(userProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(privateProfile()));
        when(userProfileMapper.toResponse(any(), any(), any(), any(), anyLong(), anyLong(), anyBoolean()))
                .thenReturn(fullResponse());

        UserProfileResponse result = service.getProfile(USER_ID, USER_ID);

        assertThat(result.isSelf()).isTrue();
        assertThat(result.restricted()).isFalse();
        assertThat(result.bio()).isEqualTo("gizli");
    }

    @Test
    void getProfile_privateProfile_anonymousIsRestricted() {
        when(userProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(privateProfile()));
        when(userProfileMapper.toResponse(any(), any(), any(), any(), anyLong(), anyLong(), anyBoolean()))
                .thenReturn(fullResponse());

        assertThat(service.getProfile(USER_ID, null).restricted()).isTrue();
    }

    @Test
    void getProfile_publicProfile_neverRestricted() {
        when(userProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

        assertThat(service.getProfile(USER_ID, 99L).restricted()).isFalse();
    }

    @Test
    void updateProfile_privateToPublic_approvesPendingRequests() {
        when(userProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(privateProfile()));
        when(userProfileRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.updateProfile(USER_ID, new UpdateUserProfileRequest(null, null, null, null, null, false));

        verify(profileFollowService).approveAllRequests(USER_ID);
    }

    @Test
    void updateProfile_publicToPrivate_keepsExistingFollowersAndApprovesNothing() {
        UserProfile existing = UserProfile.builder().id(1L).userId(USER_ID).bio("").build();
        when(userProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(existing));
        when(userProfileRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.updateProfile(USER_ID, new UpdateUserProfileRequest(null, null, null, null, null, true));

        assertThat(existing.isPrivateProfile()).isTrue();
        verify(profileFollowService, never()).approveAllRequests(any());
    }

    @Test
    void updateProfile_isPrivateNull_leavesPrivacyUnchanged() {
        UserProfile existing = privateProfile();
        when(userProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(existing));
        when(userProfileRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.updateProfile(USER_ID, new UpdateUserProfileRequest("yeni", null, null, null, null, null));

        assertThat(existing.isPrivateProfile()).isTrue();
        verify(profileFollowService, never()).approveAllRequests(any());
    }

    @Test
    void updateProfile_noExistingRow_createsAndSavesNewProfile() {
        when(userProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
        when(userProfileRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        UpdateUserProfileRequest request = new UpdateUserProfileRequest("merhaba", null, null, null, null, null);

        service.updateProfile(USER_ID, request);

        ArgumentCaptor<UserProfile> captor = ArgumentCaptor.forClass(UserProfile.class);
        verify(userProfileRepository).save(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(USER_ID);
        assertThat(captor.getValue().getBio()).isEqualTo("merhaba");
    }

    @Test
    void updateProfile_avatarUrlChanged_deletesOldImageOnlyWhenItLivesInOurAvatarsFolder() {
        UserProfile existing = UserProfile.builder().id(1L).userId(USER_ID).bio("")
                .avatarUrl("https://old/avatar.png").spoilerProtectionEnabled(false).build();
        when(userProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(existing));
        when(userProfileRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(mediaUrlValidator.isInFolder("https://old/avatar.png", "avatars")).thenReturn(true);
        UpdateUserProfileRequest request = new UpdateUserProfileRequest(
                null, "https://new/avatar.png", null, null, null, null);

        service.updateProfile(USER_ID, request);

        verify(imageStorageService).delete("https://old/avatar.png");
        assertThat(existing.getAvatarUrl()).isEqualTo("https://new/avatar.png");
    }

    @Test
    void updateProfile_oldAvatarOutsideOurFolder_isNeverDeleted_blocksArbitraryUrlDeletionTrick() {
        // Saldırı: önce avatarUrl'yi başkasının/film posterinin URL'sine ayarla, sonra başka bir değerle değiştir —
        // eski değer silinirse o görsel Cloudinary'den gider. Klasör kontrolü bunu keser.
        UserProfile existing = UserProfile.builder().id(1L).userId(USER_ID).bio("")
                .avatarUrl("https://res.cloudinary.com/x/image/upload/v1/fandoom/posters/film.png")
                .spoilerProtectionEnabled(false).build();
        when(userProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(existing));
        when(userProfileRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(mediaUrlValidator.isInFolder(any(), eq("avatars"))).thenReturn(false);

        service.updateProfile(USER_ID, new UpdateUserProfileRequest(
                null, "https://new/avatar.png", null, null, null, null));

        verify(imageStorageService, never()).delete(any());
        assertThat(existing.getAvatarUrl()).isEqualTo("https://new/avatar.png");
    }

    @Test
    void updateProfile_bannerUrlChanged_deletesOldImageOnlyWhenItLivesInOurBannersFolder() {
        UserProfile existing = UserProfile.builder().id(1L).userId(USER_ID).bio("")
                .bannerUrl("https://old/banner.png").spoilerProtectionEnabled(false).build();
        when(userProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(existing));
        when(userProfileRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(mediaUrlValidator.isInFolder("https://old/banner.png", "banners")).thenReturn(true);
        UpdateUserProfileRequest request = new UpdateUserProfileRequest(
                null, null, "https://new/banner.png", null, null, null);

        service.updateProfile(USER_ID, request);

        verify(imageStorageService).delete("https://old/banner.png");
        assertThat(existing.getBannerUrl()).isEqualTo("https://new/banner.png");
    }

    // ---- avatar / banner yükleme ----

    private MockMultipartFile image(long size) {
        return new MockMultipartFile("file", "a.png", "image/png", new byte[(int) size]);
    }

    @Test
    void uploadAvatar_usesServerFixedFolder_setsUrl_andDeletesOldOwnedAvatar() {
        UserProfile existing = UserProfile.builder().id(1L).userId(USER_ID).bio("")
                .avatarUrl("https://old/avatar.png").spoilerProtectionEnabled(false).build();
        MockMultipartFile file = image(1024);
        when(userProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(existing));
        when(userProfileRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(imageStorageService.upload(file, "avatars"))
                .thenReturn(new MediaUploadResponse("https://new/avatar.png", "fandoom/avatars/x"));
        when(mediaUrlValidator.isInFolder("https://old/avatar.png", "avatars")).thenReturn(true);

        service.uploadAvatar(USER_ID, file);

        assertThat(existing.getAvatarUrl()).isEqualTo("https://new/avatar.png");
        verify(imageStorageService).delete("https://old/avatar.png");
    }

    @Test
    void uploadBanner_usesBannersFolderAndLeavesAvatarUntouched() {
        UserProfile existing = UserProfile.builder().id(1L).userId(USER_ID).bio("")
                .avatarUrl("https://keep/avatar.png").spoilerProtectionEnabled(false).build();
        MockMultipartFile file = image(1024);
        when(userProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(existing));
        when(userProfileRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(imageStorageService.upload(file, "banners"))
                .thenReturn(new MediaUploadResponse("https://new/banner.png", "fandoom/banners/x"));

        service.uploadBanner(USER_ID, file);

        assertThat(existing.getBannerUrl()).isEqualTo("https://new/banner.png");
        assertThat(existing.getAvatarUrl()).isEqualTo("https://keep/avatar.png");
    }

    @Test
    void uploadAvatar_over5MB_isRejectedBeforeAnyUpload() {
        assertThatThrownBy(() -> service.uploadAvatar(USER_ID, image(5L * 1024 * 1024 + 1)))
                .isInstanceOf(InvalidFileException.class);

        verify(imageStorageService, never()).upload(any(), any());
        verify(userProfileRepository, never()).save(any());
    }

    @Test
    void uploadBanner_between5And8MB_isAcceptedAndOver8MBRejected() {
        MockMultipartFile ok = image(6L * 1024 * 1024);
        when(userProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
        when(userProfileRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(imageStorageService.upload(ok, "banners"))
                .thenReturn(new MediaUploadResponse("https://new/banner.png", "id"));

        service.uploadBanner(USER_ID, ok);

        assertThatThrownBy(() -> service.uploadBanner(USER_ID, image(8L * 1024 * 1024 + 1)))
                .isInstanceOf(InvalidFileException.class);
    }

    @Test
    void uploadAvatar_storageRejectsFile_propagatesAndNothingIsSaved() {
        MockMultipartFile file = image(10);
        when(imageStorageService.upload(file, "avatars")).thenThrow(new InvalidFileException("Desteklenmeyen dosya tipi"));

        assertThatThrownBy(() -> service.uploadAvatar(USER_ID, file)).isInstanceOf(InvalidFileException.class);

        verify(userProfileRepository, never()).save(any());
    }

    @Test
    void uploadAvatar_dbSaveFails_deletesTheJustUploadedImageSoNothingIsOrphaned() {
        MockMultipartFile file = image(10);
        when(userProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
        when(userProfileRepository.save(any())).thenThrow(new IllegalStateException("db down"));
        when(imageStorageService.upload(file, "avatars"))
                .thenReturn(new MediaUploadResponse("https://new/avatar.png", "id"));

        assertThatThrownBy(() -> service.uploadAvatar(USER_ID, file)).isInstanceOf(IllegalStateException.class);

        verify(imageStorageService).delete("https://new/avatar.png");
    }

    @Test
    void updateProfile_fieldsNotProvided_leaveExistingValuesUntouched() {
        UserProfile existing = UserProfile.builder().id(1L).userId(USER_ID).bio("eski bio")
                .accentColor("#111111").spoilerProtectionEnabled(true).build();
        when(userProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(existing));
        when(userProfileRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        UpdateUserProfileRequest request = new UpdateUserProfileRequest(null, null, null, null, null, null);

        service.updateProfile(USER_ID, request);

        assertThat(existing.getBio()).isEqualTo("eski bio");
        assertThat(existing.getAccentColor()).isEqualTo("#111111");
        assertThat(existing.isSpoilerProtectionEnabled()).isTrue();
    }
}
