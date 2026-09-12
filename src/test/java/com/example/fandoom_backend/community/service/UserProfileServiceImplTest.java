package com.example.fandoom_backend.community.service;

import com.example.fandoom_backend.account.entity.ActivityType;
import com.example.fandoom_backend.account.service.ActivityLogService;
import com.example.fandoom_backend.account.service.UserLikeService;
import com.example.fandoom_backend.community.dto.ProfileStats;
import com.example.fandoom_backend.community.dto.UpdateUserProfileRequest;
import com.example.fandoom_backend.community.dto.UserProfileResponse;
import com.example.fandoom_backend.community.entity.ThreadSurface;
import com.example.fandoom_backend.community.entity.UserProfile;
import com.example.fandoom_backend.community.mapper.UserProfileMapper;
import com.example.fandoom_backend.community.repository.UserProfileRepository;
import com.example.fandoom_backend.media.service.ImageStorageService;
import com.example.fandoom_backend.user.dto.UserDetailResponse;
import com.example.fandoom_backend.user.entity.Role;
import com.example.fandoom_backend.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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
    private ThreadService threadService;
    @Mock
    private CommentService commentService;

    private UserProfileServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new UserProfileServiceImpl(userProfileRepository, userLikeService, activityLogService,
                userProfileMapper, userService, imageStorageService, threadService, commentService);

        lenient().when(userService.getById(USER_ID)).thenReturn(new UserDetailResponse(
                USER_ID, "abidin", "abidin@example.com", Role.USER, true, null, false,
                LocalDateTime.of(2026, 1, 1, 0, 0)));
        lenient().when(userProfileMapper.toResponse(any(), any(), any()))
                .thenReturn(new UserProfileResponse("abidin", "", null, null, null, false,
                        new ProfileStats(0, 0, 0, 0, null)));
    }

    @Test
    void getProfile_noExistingRow_buildsTransientDefaultsWithoutPersisting() {
        when(userProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

        service.getProfile(USER_ID);

        ArgumentCaptor<UserProfile> captor = ArgumentCaptor.forClass(UserProfile.class);
        verify(userProfileMapper).toResponse(captor.capture(), eq("abidin"), any());
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

        service.getProfile(USER_ID);

        verify(userProfileMapper).toResponse(eq(existing), eq("abidin"), any());
    }

    @Test
    void getProfile_computesStatsFromLikeAndActivityRepositories() {
        when(userProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
        when(userLikeService.countByUserId(USER_ID)).thenReturn(3L);
        when(activityLogService.countByUserIdAndType(USER_ID, ActivityType.READ_BLOG)).thenReturn(5L);
        when(commentService.countByAuthorId(USER_ID)).thenReturn(2L);
        when(threadService.countByAuthorIdAndSurface(USER_ID, ThreadSurface.THEORY)).thenReturn(1L);

        service.getProfile(USER_ID);

        ArgumentCaptor<ProfileStats> statsCaptor = ArgumentCaptor.forClass(ProfileStats.class);
        verify(userProfileMapper).toResponse(any(), eq("abidin"), statsCaptor.capture());
        ProfileStats stats = statsCaptor.getValue();
        assertThat(stats.likeCount()).isEqualTo(3L);
        assertThat(stats.readBlogCount()).isEqualTo(5L);
        assertThat(stats.commentCount()).isEqualTo(2L);
        assertThat(stats.theoryCount()).isEqualTo(1L);
        assertThat(stats.memberSince()).isEqualTo(LocalDateTime.of(2026, 1, 1, 0, 0));
    }

    @Test
    void updateProfile_noExistingRow_createsAndSavesNewProfile() {
        when(userProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
        when(userProfileRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        UpdateUserProfileRequest request = new UpdateUserProfileRequest("merhaba", null, null, null, null);

        service.updateProfile(USER_ID, request);

        ArgumentCaptor<UserProfile> captor = ArgumentCaptor.forClass(UserProfile.class);
        verify(userProfileRepository).save(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(USER_ID);
        assertThat(captor.getValue().getBio()).isEqualTo("merhaba");
    }

    @Test
    void updateProfile_avatarUrlChanged_deletesOldImageAndSetsNew() {
        UserProfile existing = UserProfile.builder().id(1L).userId(USER_ID).bio("")
                .avatarUrl("https://old/avatar.png").spoilerProtectionEnabled(false).build();
        when(userProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(existing));
        when(userProfileRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        UpdateUserProfileRequest request = new UpdateUserProfileRequest(
                null, "https://new/avatar.png", null, null, null);

        service.updateProfile(USER_ID, request);

        verify(imageStorageService).deleteIfChanged("https://old/avatar.png", "https://new/avatar.png");
        assertThat(existing.getAvatarUrl()).isEqualTo("https://new/avatar.png");
    }

    @Test
    void updateProfile_bannerUrlChanged_deletesOldImageAndSetsNew() {
        UserProfile existing = UserProfile.builder().id(1L).userId(USER_ID).bio("")
                .bannerUrl("https://old/banner.png").spoilerProtectionEnabled(false).build();
        when(userProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(existing));
        when(userProfileRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        UpdateUserProfileRequest request = new UpdateUserProfileRequest(
                null, null, "https://new/banner.png", null, null);

        service.updateProfile(USER_ID, request);

        verify(imageStorageService).deleteIfChanged("https://old/banner.png", "https://new/banner.png");
        assertThat(existing.getBannerUrl()).isEqualTo("https://new/banner.png");
    }

    @Test
    void updateProfile_fieldsNotProvided_leaveExistingValuesUntouched() {
        UserProfile existing = UserProfile.builder().id(1L).userId(USER_ID).bio("eski bio")
                .accentColor("#111111").spoilerProtectionEnabled(true).build();
        when(userProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(existing));
        when(userProfileRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        UpdateUserProfileRequest request = new UpdateUserProfileRequest(null, null, null, null, null);

        service.updateProfile(USER_ID, request);

        assertThat(existing.getBio()).isEqualTo("eski bio");
        assertThat(existing.getAccentColor()).isEqualTo("#111111");
        assertThat(existing.isSpoilerProtectionEnabled()).isTrue();
    }
}
