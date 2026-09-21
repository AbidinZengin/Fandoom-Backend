package com.example.fandoom_backend.community.controller;

import com.example.fandoom_backend.common.dto.PageResponse;
import com.example.fandoom_backend.common.exception.ResourceNotFoundException;
import com.example.fandoom_backend.community.dto.PortalDetailResponse;
import com.example.fandoom_backend.community.dto.PortalSummaryResponse;
import com.example.fandoom_backend.community.entity.PortalPostingPolicy;
import com.example.fandoom_backend.community.entity.PortalStatus;
import com.example.fandoom_backend.community.entity.ThreadSurface;
import com.example.fandoom_backend.community.service.CommunityFeedService;
import com.example.fandoom_backend.community.service.PortalQueryService;
import com.example.fandoom_backend.user.entity.Role;
import com.example.fandoom_backend.user.entity.User;
import com.example.fandoom_backend.user.security.CustomUserDetails;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// B2: public portal okuma uçları (dizin, detay, portal feed'i). Gerçek SecurityConfig; servisler @MockitoBean.
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {"MAIL_HOST=localhost", "MAIL_USERNAME=test", "MAIL_PASSWORD=test"})
class PortalControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private PortalQueryService portalQueryService;
    @MockitoBean private CommunityFeedService communityFeedService;

    private static RequestPostProcessor asUser(Long id) {
        User user = User.builder().id(id).username("u" + id).password("x").role(Role.USER).emailVerified(true).build();
        CustomUserDetails principal = new CustomUserDetails(user);
        return SecurityMockMvcRequestPostProcessors.authentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    private static PortalSummaryResponse summary(boolean member) {
        return new PortalSummaryResponse(1L, "westeros", "Westeros", "açıklama", null, null, null, 12, 34, member,
                PortalPostingPolicy.OPEN, PortalStatus.ACTIVE, List.of("game-of-thrones", "house-of-the-dragon"));
    }

    @Test
    void list_isPublic_defaultsToTrending_andReturnsTheContractShape() throws Exception {
        when(portalQueryService.list(anyString(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageResponse<>(List.of(summary(false)), 0, 20, 1, 1, true));

        mockMvc.perform(get("/api/community/portals"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].slug").value("westeros"))
                .andExpect(jsonPath("$.content[0].isMember").value(false))
                .andExpect(jsonPath("$.content[0].memberCount").value(12))
                .andExpect(jsonPath("$.content[0].postingPolicy").value("OPEN"))
                .andExpect(jsonPath("$.content[0].productionSlugs[1]").value("house-of-the-dragon"))
                .andExpect(jsonPath("$.totalElements").value(1));

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(portalQueryService).list(eq("trending"), isNull(), isNull(), pageable.capture());
        assertThat(pageable.getValue().getPageSize()).isEqualTo(20);
    }

    @Test
    void list_passesSortQueryPagingAndViewer() throws Exception {
        when(portalQueryService.list(anyString(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageResponse<>(List.of(summary(true)), 1, 5, 6, 2, true));

        mockMvc.perform(get("/api/community/portals").param("sort", "members").param("q", "west")
                        .param("page", "1").param("size", "5").with(asUser(7L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].isMember").value(true));

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(portalQueryService).list(eq("members"), eq("west"), eq(7L), pageable.capture());
        assertThat(pageable.getValue().getPageNumber()).isEqualTo(1);
        assertThat(pageable.getValue().getPageSize()).isEqualTo(5);
    }

    @Test
    void detail_isPublic_hiddenOrMissingIs404() throws Exception {
        when(portalQueryService.getBySlug("westeros", null)).thenReturn(new PortalDetailResponse(1L, "westeros",
                "Westeros", null, null, null, "#1A2B3C", 12, 34, false, PortalPostingPolicy.OPEN, PortalStatus.ACTIVE, List.of(),
                LocalDateTime.of(2026, 9, 1, 10, 0)));
        when(portalQueryService.getBySlug("gizli", null)).thenThrow(new ResourceNotFoundException("Portal bulunamadı"));

        mockMvc.perform(get("/api/community/portals/westeros"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.accentColor").value("#1A2B3C"));
        mockMvc.perform(get("/api/community/portals/gizli")).andExpect(status().isNotFound());
    }

    @Test
    void portalFeed_isTheSameServiceCallAsFeedWithPortalParam() throws Exception {
        when(communityFeedService.getFeed(any(), any(), any(), any(), anyString(), any(), any(Pageable.class)))
                .thenReturn(new PageResponse<>(List.of(), 0, 20, 0, 0, true));

        mockMvc.perform(get("/api/community/portals/westeros/feed").param("surface", "THEORY").param("sort", "hot"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/community/feed").param("portal", "westeros").param("surface", "THEORY").param("sort", "hot"))
                .andExpect(status().isOk());

        // her iki uç de birebir aynı çağrıyı üretir -> aynı sonuç
        org.mockito.Mockito.verify(communityFeedService, org.mockito.Mockito.times(2)).getFeed(
                eq(ThreadSurface.THEORY), eq("westeros"), isNull(), isNull(), eq("hot"), isNull(), any(Pageable.class));
    }

    @Test
    void portalFeed_unknownPortalIs404_viewerIdIsPassed() throws Exception {
        when(communityFeedService.getFeed(any(), eq("gizli"), any(), any(), anyString(), any(), any(Pageable.class)))
                .thenThrow(new ResourceNotFoundException("Portal bulunamadı"));
        when(communityFeedService.getFeed(any(), eq("westeros"), any(), any(), anyString(), eq(7L), any(Pageable.class)))
                .thenReturn(new PageResponse<>(List.of(), 0, 20, 0, 0, true));

        mockMvc.perform(get("/api/community/portals/gizli/feed")).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/community/portals/westeros/feed").with(asUser(7L))).andExpect(status().isOk());
    }
}
