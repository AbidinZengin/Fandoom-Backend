package com.example.fandoom_backend.community.controller;

import com.example.fandoom_backend.common.dto.KeysetPageResponse;
import com.example.fandoom_backend.common.dto.PageResponse;
import com.example.fandoom_backend.common.exception.InvalidReferenceException;
import com.example.fandoom_backend.common.exception.ResourceNotFoundException;
import com.example.fandoom_backend.community.dto.PortalMembershipStatusResponse;
import com.example.fandoom_backend.community.dto.PortalSummaryResponse;
import com.example.fandoom_backend.community.dto.ThreadDetailResponse;
import com.example.fandoom_backend.community.dto.ThreadPatchRequest;
import com.example.fandoom_backend.community.entity.PortalPostingPolicy;
import com.example.fandoom_backend.community.entity.PortalStatus;
import com.example.fandoom_backend.community.entity.ThreadSurface;
import com.example.fandoom_backend.community.service.CommunityFeedService;
import com.example.fandoom_backend.community.service.PortalMembershipService;
import com.example.fandoom_backend.community.service.PortalQueryService;
import com.example.fandoom_backend.community.service.ThreadInteractionService;
import com.example.fandoom_backend.community.service.ThreadService;
import com.example.fandoom_backend.user.entity.Role;
import com.example.fandoom_backend.user.entity.User;
import com.example.fandoom_backend.user.security.CustomUserDetails;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// B3: üyelik uçları, /api/me/portals, feed scope=joined (401/200/400) ve PATCH/DELETE id-slug yönlendirmesi.
// Gerçek SecurityConfig + filtre zinciri; servisler @MockitoBean.
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {"MAIL_HOST=localhost", "MAIL_USERNAME=test", "MAIL_PASSWORD=test"})
@SuppressWarnings("deprecation")
class PortalMembershipControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private PortalMembershipService portalMembershipService;
    @MockitoBean private PortalQueryService portalQueryService;
    @MockitoBean private CommunityFeedService communityFeedService;
    @MockitoBean private ThreadService threadService;
    @MockitoBean private ThreadInteractionService threadInteractionService;

    private static RequestPostProcessor asUser(Long id, Role role) {
        User user = User.builder().id(id).username("u" + id).password("x").role(role).emailVerified(true).build();
        CustomUserDetails principal = new CustomUserDetails(user);
        return SecurityMockMvcRequestPostProcessors.authentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    // ---- join / leave ----

    @Test
    void join_requiresAuth_returnsMemberAndCount() throws Exception {
        mockMvc.perform(post("/api/community/portals/westeros/join")).andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/community/portals/westeros/join")).andExpect(status().isUnauthorized());

        when(portalMembershipService.join(7L, "westeros")).thenReturn(new PortalMembershipStatusResponse(true, 5));
        when(portalMembershipService.leave(7L, "westeros")).thenReturn(new PortalMembershipStatusResponse(false, 4));

        mockMvc.perform(post("/api/community/portals/westeros/join").with(asUser(7L, Role.USER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.member").value(true))
                .andExpect(jsonPath("$.memberCount").value(5));
        mockMvc.perform(delete("/api/community/portals/westeros/join").with(asUser(7L, Role.USER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.member").value(false))
                .andExpect(jsonPath("$.memberCount").value(4));
    }

    @Test
    void join_archivedIs400_hiddenIs404() throws Exception {
        when(portalMembershipService.join(7L, "arsiv")).thenThrow(new InvalidReferenceException("arşiv"));
        when(portalMembershipService.join(7L, "gizli")).thenThrow(new ResourceNotFoundException("gizli"));

        mockMvc.perform(post("/api/community/portals/arsiv/join").with(asUser(7L, Role.USER))).andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/community/portals/gizli/join").with(asUser(7L, Role.USER))).andExpect(status().isNotFound());
    }

    // ---- /api/me/portals ----

    @Test
    void myPortals_requiresAuth_returnsPlainList_andPassesSort() throws Exception {
        mockMvc.perform(get("/api/me/portals")).andExpect(status().isUnauthorized());

        when(portalQueryService.listJoined(eq(7L), any())).thenReturn(List.of(new PortalSummaryResponse(1L, "westeros",
                "Westeros", null, null, null, null, 5, 2, true, PortalPostingPolicy.OPEN, PortalStatus.ARCHIVED, List.of())));

        mockMvc.perform(get("/api/me/portals").with(asUser(7L, Role.USER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].slug").value("westeros"))
                .andExpect(jsonPath("$[0].isMember").value(true))
                .andExpect(jsonPath("$[0].status").value("ARCHIVED"));
        mockMvc.perform(get("/api/me/portals").param("sort", "activity").with(asUser(7L, Role.USER)))
                .andExpect(status().isOk());

        verify(portalQueryService).listJoined(7L, null);
        verify(portalQueryService).listJoined(7L, "activity");
    }

    // ---- feed scope=joined ----

    @Test
    void feedScopeJoined_anonymousIs401_onBothFeedEndpoints_caseInsensitive() throws Exception {
        mockMvc.perform(get("/api/community/feed").param("scope", "joined")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/community/feed").param("scope", "JOINED")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/community/feed/cursor").param("scope", "joined")).andExpect(status().isUnauthorized());

        verify(communityFeedService, never()).getJoinedFeed(anyLong(), any(), any(), any(), any(), anyString(), any());
    }

    @Test
    void feedScopeJoined_authenticated_usesJoinedFeedWithViewerAndCombinesFilters() throws Exception {
        when(communityFeedService.getJoinedFeed(anyLong(), any(), any(), any(), any(), anyString(), any(Pageable.class)))
                .thenReturn(new PageResponse<>(List.of(), 0, 20, 0, 0, true));
        when(communityFeedService.getJoinedFeedByCursor(anyLong(), any(), any(), any(), any(), anyString(), any(), org.mockito.ArgumentMatchers.anyInt()))
                .thenReturn(new KeysetPageResponse<>(List.of(), false, null));

        mockMvc.perform(get("/api/community/feed").param("scope", "joined").param("portal", "westeros")
                        .param("surface", "THEORY").param("sort", "new").with(asUser(7L, Role.USER)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/community/feed/cursor").param("scope", "joined").with(asUser(7L, Role.USER)))
                .andExpect(status().isOk());

        verify(communityFeedService).getJoinedFeed(eq(7L), eq(ThreadSurface.THEORY), eq("westeros"), isNull(), isNull(),
                eq("new"), any(Pageable.class));
        verify(communityFeedService).getJoinedFeedByCursor(eq(7L), isNull(), isNull(), isNull(), isNull(), eq("hot"), isNull(), eq(20));
        verify(communityFeedService, never()).getFeed(any(), any(), any(), any(), anyString(), any(), any(Pageable.class));
    }

    @Test
    void feedScopeAll_isPublic_defaultIsAll_bogusScopeIs400() throws Exception {
        when(communityFeedService.getFeed(any(), any(), any(), any(), anyString(), any(), any(Pageable.class)))
                .thenReturn(new PageResponse<>(List.of(), 0, 20, 0, 0, true));

        mockMvc.perform(get("/api/community/feed")).andExpect(status().isOk());
        mockMvc.perform(get("/api/community/feed").param("scope", "all")).andExpect(status().isOk());
        mockMvc.perform(get("/api/community/feed").param("scope", "friends")).andExpect(status().isBadRequest());

        verify(communityFeedService, never()).getJoinedFeed(anyLong(), any(), any(), any(), any(), anyString(), any());
    }

    // ---- PATCH / DELETE id ↔ slug ----

    private static ThreadDetailResponse detail() {
        return new ThreadDetailResponse(10L, "slug", ThreadSurface.THEORY, "Başlık", "b", null, List.of(), false, 1L, null,
                null, 0, 0, 0, false, false, List.of(), null, null, null);
    }

    @Test
    void patch_numericSegmentIsId_slugSegmentIsDeprecatedSlugVariant_portalSlugIsPassedThrough() throws Exception {
        when(threadService.update(anyLong(), org.mockito.ArgumentMatchers.anyBoolean(), anyLong(), any(ThreadPatchRequest.class)))
                .thenReturn(detail());
        when(threadService.update(anyLong(), org.mockito.ArgumentMatchers.anyBoolean(), anyString(), any(ThreadPatchRequest.class)))
                .thenReturn(detail());
        String body = "{\"portalSlug\":\"genel-sohbet\"}";

        mockMvc.perform(patch("/api/community/threads/1234567890").with(asUser(8L, Role.MODERATOR))
                        .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isOk());
        mockMvc.perform(patch("/api/community/threads/eski-baslik").with(asUser(8L, Role.USER))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"Yeni bir başlık burada\"}"))
                .andExpect(status().isOk());

        ArgumentCaptor<ThreadPatchRequest> request = ArgumentCaptor.forClass(ThreadPatchRequest.class);
        verify(threadService).update(eq(8L), eq(true), eq(1234567890L), request.capture());
        assertThat(request.getValue().portalSlug()).isEqualTo("genel-sohbet");
        verify(threadService).update(eq(8L), eq(false), eq("eski-baslik"), any(ThreadPatchRequest.class));
    }

    @Test
    void patch_ownerMovingPortalIs403_anonymousIs401() throws Exception {
        when(threadService.update(anyLong(), org.mockito.ArgumentMatchers.anyBoolean(), anyLong(), any(ThreadPatchRequest.class)))
                .thenThrow(new AccessDeniedException("taşıma yetkisi yok"));

        mockMvc.perform(patch("/api/community/threads/10").with(asUser(1L, Role.USER))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"portalSlug\":\"baska\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch("/api/community/threads/10")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"portalSlug\":\"baska\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void delete_byIdAndBySlug_routeToTheirVariants() throws Exception {
        mockMvc.perform(delete("/api/community/threads/10").with(asUser(1L, Role.USER))).andExpect(status().isNoContent());
        mockMvc.perform(delete("/api/community/threads/eski-baslik").with(asUser(1L, Role.USER))).andExpect(status().isNoContent());
        mockMvc.perform(delete("/api/community/threads/10")).andExpect(status().isUnauthorized());

        verify(threadService).delete(1L, false, 10L);
        verify(threadService).delete(1L, false, "eski-baslik");
    }
}
