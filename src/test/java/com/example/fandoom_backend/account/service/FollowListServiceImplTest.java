package com.example.fandoom_backend.account.service;

import com.example.fandoom_backend.account.dto.FollowUserResponse;
import com.example.fandoom_backend.common.dto.PageResponse;
import com.example.fandoom_backend.community.dto.ProfileCard;
import com.example.fandoom_backend.community.service.UserProfileService;
import com.example.fandoom_backend.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FollowListServiceImplTest {

    private static final Long VIEWER = 99L;
    private static final Long ARYA = 42L;
    private static final Long ZED = 43L;
    private static final Pageable PAGE = PageRequest.of(0, 20);

    @Mock
    private ProfileFollowService profileFollowService;
    @Mock
    private UserService userService;
    @Mock
    private UserProfileService userProfileService;
    @InjectMocks
    private FollowListServiceImpl service;

    private PageResponse<Long> ids(Long... ids) {
        return new PageResponse<>(List.of(ids), 0, 20, ids.length, 1, true);
    }

    private void stubUsers(Long viewer, Set<Long> followedByViewer, Map<Long, ProfileCard> cards, Long... ids) {
        Set<Long> all = Set.of(ids);
        when(userService.searchPublicIds(null, PAGE)).thenReturn(ids(ids));
        when(userService.getUsernamesByIds(all)).thenReturn(
                Map.of(ARYA, "arya", ZED, "zed"));
        when(userProfileService.getCardsByUserIds(all)).thenReturn(cards);
        when(profileFollowService.filterFollowedBy(viewer, all)).thenReturn(followedByViewer);
    }

    @Test
    void users_privateProfileNotFollowedByViewer_hidesBioAndAccentButKeepsAvatar() {
        stubUsers(VIEWER, Set.of(), Map.of(ARYA, new ProfileCard("a.png", "gizli bio", "#112233", true)), ARYA);

        FollowUserResponse row = service.users(null, VIEWER, PAGE).content().get(0);

        assertThat(row.username()).isEqualTo("arya");
        assertThat(row.avatarUrl()).isEqualTo("a.png");
        assertThat(row.bio()).isNull();
        assertThat(row.accentColor()).isNull();
        assertThat(row.following()).isFalse();
    }

    @Test
    void users_privateProfileFollowedByViewer_showsBio() {
        stubUsers(VIEWER, Set.of(ARYA), Map.of(ARYA, new ProfileCard("a.png", "gizli bio", "#112233", true)), ARYA);

        FollowUserResponse row = service.users(null, VIEWER, PAGE).content().get(0);

        assertThat(row.bio()).isEqualTo("gizli bio");
        assertThat(row.accentColor()).isEqualTo("#112233");
        assertThat(row.following()).isTrue();
    }

    @Test
    void users_privateProfileAnonymousViewer_hidesBio() {
        stubUsers(null, Set.of(), Map.of(ARYA, new ProfileCard("a.png", "gizli bio", "#112233", true)), ARYA);

        assertThat(service.users(null, null, PAGE).content().get(0).bio()).isNull();
    }

    @Test
    void users_ownPrivateProfile_viewerSeesOwnBio() {
        stubUsers(ARYA, Set.of(), Map.of(ARYA, new ProfileCard("a.png", "gizli bio", "#112233", true)), ARYA);

        assertThat(service.users(null, ARYA, PAGE).content().get(0).bio()).isEqualTo("gizli bio");
    }

    @Test
    void users_publicProfile_alwaysShowsBio_andLongBioIsTruncatedTo160() {
        String longBio = "x".repeat(300);
        stubUsers(null, Set.of(), Map.of(ARYA, new ProfileCard(null, longBio, null, false)), ARYA);

        assertThat(service.users(null, null, PAGE).content().get(0).bio()).hasSize(160);
    }

    @Test
    void users_userWithoutProfileRow_hasNullProfileFieldsButStillListed() {
        stubUsers(VIEWER, Set.of(), Map.of(), ARYA);

        FollowUserResponse row = service.users(null, VIEWER, PAGE).content().get(0);

        assertThat(row.username()).isEqualTo("arya");
        assertThat(row.avatarUrl()).isNull();
        assertThat(row.bio()).isNull();
    }

    @Test
    void followers_restrictedViewer_getsEmptyPageWithoutQueryingFollowers() {
        when(userService.getIdByUsername("arya")).thenReturn(ARYA);
        when(profileFollowService.isRestricted(VIEWER, ARYA)).thenReturn(true);

        PageResponse<FollowUserResponse> result = service.followers("arya", VIEWER, PAGE);

        assertThat(result.content()).isEmpty();
        verify(profileFollowService, never()).listFollowerIds(any(), any());
    }

    @Test
    void followers_followerOfPrivateProfile_privateFollowerBioIsMaskedForOutsiders() {
        // Arya herkese açık; takipçisi Zed gizli. Arya'nın takipçi listesine bakan yabancı Zed'in bio'sunu görmemeli.
        when(userService.getIdByUsername("arya")).thenReturn(ARYA);
        when(profileFollowService.isRestricted(VIEWER, ARYA)).thenReturn(false);
        when(profileFollowService.listFollowerIds(ARYA, PAGE)).thenReturn(ids(ZED));
        when(userService.getUsernamesByIds(Set.of(ZED))).thenReturn(Map.of(ZED, "zed"));
        when(userProfileService.getCardsByUserIds(Set.of(ZED)))
                .thenReturn(Map.of(ZED, new ProfileCard("z.png", "zed gizli", "#000000", true)));
        when(profileFollowService.filterFollowedBy(VIEWER, Set.of(ZED))).thenReturn(Set.of());

        FollowUserResponse row = service.followers("arya", VIEWER, PAGE).content().get(0);

        assertThat(row.username()).isEqualTo("zed");
        assertThat(row.avatarUrl()).isEqualTo("z.png");
        assertThat(row.bio()).isNull();
    }
}
