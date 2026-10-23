package com.example.fandoom_backend.account.controller;

import com.example.fandoom_backend.account.dto.FollowStateResponse;
import com.example.fandoom_backend.account.entity.FollowState;
import com.example.fandoom_backend.account.service.FollowListService;
import com.example.fandoom_backend.account.service.ProfileFollowService;
import com.example.fandoom_backend.common.dto.PageResponse;
import com.example.fandoom_backend.community.service.ProfileContentService;
import com.example.fandoom_backend.notification.service.NotificationService;
import com.example.fandoom_backend.user.entity.Role;
import com.example.fandoom_backend.user.entity.User;
import com.example.fandoom_backend.user.security.CustomUserDetails;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Kişi takibi / gizli profil / bildirimler / profil sekmeleri uçları: GERÇEK SecurityConfig + gerçek controller'lar,
// servisler @MockitoBean ile izole (DB iş mantığına girmez). Neyi kanıtlar: (1) herkese açık olması gereken uçlar anonim
// istekle 200 verir — SecurityConfig'te yol kuralı eksik olsaydı /api/users/** ADMIN kuralına ya da
// anyRequest().authenticated()'a düşüp 401/403 olurdu; (2) /api/me/** uçları girişsiz 401 verir; (3) principal'daki
// userId (JWT) servise taşınır, istekten alınmaz; (4) /api/users/{id} (admin) hâlâ kapalıdır.
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "MAIL_HOST=localhost",
        "MAIL_USERNAME=test",
        "MAIL_PASSWORD=test"
})
class ProfileEndpointsSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProfileFollowService profileFollowService;
    @MockitoBean
    private FollowListService followListService;
    @MockitoBean
    private ProfileContentService profileContentService;
    @MockitoBean
    private NotificationService notificationService;

    // ---- herkese açık uçlar: anonim istek 200 ----

    @Test
    void publicProfileTabs_anonymousGets200() throws Exception {
        for (String path : List.of(
                "/api/users/arya/followers", "/api/users/arya/following", "/api/users/arya/comments",
                "/api/users/arya/lists", "/api/users/arya/lists/pinned", "/api/community/users")) {
            mockMvc.perform(get(path)).andExpect(status().isOk());
        }
    }

    @Test
    void userDirectory_anonymous_passesQueryAndNullViewer() throws Exception {
        mockMvc.perform(get("/api/community/users").param("q", "ar")).andExpect(status().isOk());

        verify(followListService).users(eq("ar"), isNull(), any());
    }

    @Test
    void userDirectory_authenticated_passesViewerId() throws Exception {
        mockMvc.perform(get("/api/community/users").with(asUser(7L, "abidin", Role.USER)))
                .andExpect(status().isOk());

        verify(followListService).users(isNull(), eq(7L), any());
    }

    @Test
    void threadsAuthorFilter_isPublic_andRoutesToProfileContentService() throws Exception {
        mockMvc.perform(get("/api/community/threads").param("author", "arya").param("sort", "new"))
                .andExpect(status().isOk());

        verify(profileContentService).threads(eq("arya"), isNull(), eq("new"), isNull(), any());
    }

    @Test
    void feedAuthorFilter_isPublic_andRoutesToProfileContentService() throws Exception {
        mockMvc.perform(get("/api/community/feed").param("author", "arya")).andExpect(status().isOk());

        verify(profileContentService).threads(eq("arya"), isNull(), eq("hot"), isNull(), any());
    }

    @Test
    void followersList_authenticatedViewerIdReachesService() throws Exception {
        when(followListService.followers(eq("arya"), eq(7L), any()))
                .thenReturn(new PageResponse<>(List.of(), 0, 20, 0, 0, true));

        mockMvc.perform(get("/api/users/arya/followers").with(asUser(7L, "abidin", Role.USER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    // ---- /api/me/** girişsiz 401 ----

    @Test
    void meEndpoints_withoutAuth_returnUnauthorizedAndTouchNoService() throws Exception {
        mockMvc.perform(post("/api/me/following/arya")).andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/me/following/arya")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/me/following/arya")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/me/follow-requests")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/me/follow-requests/arya/accept")).andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/me/follow-requests/arya")).andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/me/followers/arya")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/me/notifications")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/me/notifications/unread-count")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/me/notifications/1/read")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/me/notifications/read-all")).andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/me/notifications/1")).andExpect(status().isUnauthorized());

        verifyNoInteractions(profileFollowService, notificationService, followListService);
    }

    // ---- kişi takibi: principal userId'si servise taşınır ----

    @Test
    void follow_authenticated_usesPrincipalIdAndReturnsState() throws Exception {
        when(profileFollowService.follow(7L, "arya"))
                .thenReturn(new FollowStateResponse(false, FollowState.REQUESTED, 3L));

        mockMvc.perform(post("/api/me/following/arya").with(asUser(7L, "abidin", Role.USER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.following").value(false))
                .andExpect(jsonPath("$.status").value("REQUESTED"))
                .andExpect(jsonPath("$.followerCount").value(3));
    }

    @Test
    void unfollowAndState_authenticated_delegateWithPrincipalId() throws Exception {
        when(profileFollowService.unfollow(7L, "arya"))
                .thenReturn(new FollowStateResponse(false, FollowState.NONE, 0L));
        when(profileFollowService.getState(7L, "arya"))
                .thenReturn(new FollowStateResponse(true, FollowState.FOLLOWING, 1L));

        mockMvc.perform(delete("/api/me/following/arya").with(asUser(7L, "abidin", Role.USER)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("NONE"));
        mockMvc.perform(get("/api/me/following/arya").with(asUser(7L, "abidin", Role.USER)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.following").value(true));
    }

    @Test
    void followRequestEndpoints_authenticated_return204AndDelegateWithPrincipalId() throws Exception {
        mockMvc.perform(post("/api/me/follow-requests/arya/accept").with(asUser(7L, "abidin", Role.USER)))
                .andExpect(status().isNoContent());
        mockMvc.perform(delete("/api/me/follow-requests/arya").with(asUser(7L, "abidin", Role.USER)))
                .andExpect(status().isNoContent());
        mockMvc.perform(delete("/api/me/followers/arya").with(asUser(7L, "abidin", Role.USER)))
                .andExpect(status().isNoContent());

        verify(profileFollowService).acceptRequest(7L, "arya");
        verify(profileFollowService).rejectRequest(7L, "arya");
        verify(profileFollowService).removeFollower(7L, "arya");
    }

    @Test
    void followRequestsList_authenticated_usesPrincipalIdAsProfileOwner() throws Exception {
        mockMvc.perform(get("/api/me/follow-requests").with(asUser(7L, "abidin", Role.USER)))
                .andExpect(status().isOk());

        verify(followListService).pendingRequests(eq(7L), any());
    }

    // ---- bildirimler ----

    @Test
    void notifications_authenticated_delegateWithPrincipalId() throws Exception {
        when(notificationService.unreadCount(7L)).thenReturn(3L);

        mockMvc.perform(get("/api/me/notifications").param("unreadOnly", "true")
                        .with(asUser(7L, "abidin", Role.USER)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/me/notifications/unread-count").with(asUser(7L, "abidin", Role.USER)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.unreadCount").value(3));
        mockMvc.perform(post("/api/me/notifications/5/read").with(asUser(7L, "abidin", Role.USER)))
                .andExpect(status().isNoContent());
        mockMvc.perform(post("/api/me/notifications/read-all").with(asUser(7L, "abidin", Role.USER)))
                .andExpect(status().isNoContent());
        mockMvc.perform(delete("/api/me/notifications/5").with(asUser(7L, "abidin", Role.USER)))
                .andExpect(status().isNoContent());

        verify(notificationService).list(eq(7L), eq(true), any());
        verify(notificationService).markRead(7L, 5L);
        verify(notificationService).markAllRead(7L);
        verify(notificationService).delete(7L, 5L);
    }

    // ---- regresyon: kapalı olması gerekenler kapalı kalmalı ----

    @Test
    void adminUserEndpoints_stayClosedToAnonymousAndPlainUsers() throws Exception {
        mockMvc.perform(get("/api/users/5")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/users/5").with(asUser(7L, "abidin", Role.USER)))
                .andExpect(status().isForbidden());
    }

    private RequestPostProcessor asUser(Long id, String username, Role role) {
        User user = User.builder().id(id).username(username).password("x").role(role).emailVerified(true).build();
        CustomUserDetails principal = new CustomUserDetails(user);
        return SecurityMockMvcRequestPostProcessors.authentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }
}
