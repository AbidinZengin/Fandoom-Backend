package com.example.fandoom_backend.community.controller;

import com.example.fandoom_backend.common.dto.PageResponse;
import com.example.fandoom_backend.common.exception.ResourceNotFoundException;
import com.example.fandoom_backend.community.dto.AdminPortalResponse;
import com.example.fandoom_backend.community.dto.PortalCreateRequest;
import com.example.fandoom_backend.community.dto.PortalRefResponse;
import com.example.fandoom_backend.community.dto.ThreadBookmarkStatusResponse;
import com.example.fandoom_backend.community.dto.ThreadDetailResponse;
import com.example.fandoom_backend.community.dto.ThreadLikeStatusResponse;
import com.example.fandoom_backend.community.dto.ThreadRequest;
import com.example.fandoom_backend.community.entity.PortalPostingPolicy;
import com.example.fandoom_backend.community.entity.PortalStatus;
import com.example.fandoom_backend.community.entity.ThreadSurface;
import com.example.fandoom_backend.community.service.CommentInteractionService;
import com.example.fandoom_backend.community.service.CommentService;
import com.example.fandoom_backend.community.service.CommunityFeedService;
import com.example.fandoom_backend.community.service.PortalAdminService;
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
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
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

// Tam Spring context (gerçek SecurityConfig + filtre zinciri + gerçek path pattern'leri) — AccountControllerTest /
// BlogControllerTest deseni. Servisler @MockitoBean; DB'ye iş mantığı için dokunmaz. Doğrulananlar:
//  - {id:\d+} ↔ {slug:(?!\d+$).+} yönlendirmesi (saf-sayısal slug dahil) ve id varyantlarının yetkilendirmesi,
//  - POST /threads'te portalSlug zorunluluğu (400 fieldErrors) ve moderator bayrağının servise geçişi,
//  - feed'de portal/productionSlug/tags parametreleri,
//  - /api/admin/community/** yalnız ADMIN.
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "MAIL_HOST=localhost",
        "MAIL_USERNAME=test",
        "MAIL_PASSWORD=test"
})
@SuppressWarnings("deprecation")
class PortalThreadControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private ThreadService threadService;
    @MockitoBean private ThreadInteractionService threadInteractionService;
    @MockitoBean private CommentService commentService;
    @MockitoBean private CommentInteractionService commentInteractionService;
    @MockitoBean private CommunityFeedService communityFeedService;
    @MockitoBean private PortalAdminService portalAdminService;

    private static RequestPostProcessor asUser(Long id, Role role) {
        User user = User.builder().id(id).username("u" + id).password("x").role(role).emailVerified(true).build();
        CustomUserDetails principal = new CustomUserDetails(user);
        return SecurityMockMvcRequestPostProcessors.authentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    private static ThreadDetailResponse detail() {
        return new ThreadDetailResponse(10L, "baslik-slug", ThreadSurface.THEORY, "Başlık", "gövde", null, List.of(),
                false, 1L, null, null, 0, 0, 0, false, false, List.of(), LocalDateTime.of(2026, 9, 20, 10, 0), null,
                new PortalRefResponse("westeros", "Westeros"));
    }

    // ---- GET /threads/{id} ----

    @Test
    void getThreadById_anonymous_isPublic_andReturnsIdSlugAndPortal() throws Exception {
        when(threadService.getById(10L, null)).thenReturn(detail());

        mockMvc.perform(get("/api/community/threads/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.slug").value("baslik-slug"))
                .andExpect(jsonPath("$.portal.slug").value("westeros"))
                .andExpect(jsonPath("$.portal.name").value("Westeros"));
    }

    @Test
    void getThread_numericSegmentIsAlwaysAnId_evenIfAThreadHasAPureNumericSlug() throws Exception {
        when(threadService.getById(1234567890L, null)).thenReturn(detail());

        mockMvc.perform(get("/api/community/threads/1234567890")).andExpect(status().isOk());

        verify(threadService).getById(1234567890L, null);
        verify(threadService, never()).getBySlug(anyString(), any());
    }

    @Test
    void getThread_nonNumericSegment_stillRoutesToDeprecatedSlugEndpoint() throws Exception {
        when(threadService.getBySlug("eski-baslik", null)).thenReturn(detail());

        mockMvc.perform(get("/api/community/threads/eski-baslik")).andExpect(status().isOk());

        verify(threadService).getBySlug("eski-baslik", null);
        verify(threadService, never()).getById(anyLong(), any());
    }

    @Test
    void getThread_slugWithDigitsButNotPureNumeric_isASlug() throws Exception {
        when(threadService.getBySlug("2024-teorileri", null)).thenReturn(detail());

        mockMvc.perform(get("/api/community/threads/2024-teorileri")).andExpect(status().isOk());

        verify(threadService).getBySlug("2024-teorileri", null);
    }

    @Test
    void getThreadById_notFoundHiddenOrDeleted_is404() throws Exception {
        when(threadService.getById(99L, null)).thenThrow(new ResourceNotFoundException("Thread bulunamadı: id=99"));

        mockMvc.perform(get("/api/community/threads/99")).andExpect(status().isNotFound());
    }

    // ---- like / bookmark / comments (id varyantları) ----

    @Test
    void likeById_requiresAuth_andRoutesToIdVariant() throws Exception {
        mockMvc.perform(post("/api/community/threads/10/like")).andExpect(status().isUnauthorized());

        when(threadInteractionService.like(7L, 10L)).thenReturn(new ThreadLikeStatusResponse(true, 3));
        mockMvc.perform(post("/api/community/threads/10/like").with(asUser(7L, Role.USER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.likeCount").value(3));
        verify(threadInteractionService).like(7L, 10L);
    }

    @Test
    void unlike_bookmark_unbookmark_byId_routeToIdVariants() throws Exception {
        when(threadInteractionService.unlike(7L, 10L)).thenReturn(new ThreadLikeStatusResponse(false, 2));
        when(threadInteractionService.bookmark(7L, 10L)).thenReturn(new ThreadBookmarkStatusResponse(true, 1));
        when(threadInteractionService.unbookmark(7L, 10L)).thenReturn(new ThreadBookmarkStatusResponse(false, 0));

        mockMvc.perform(delete("/api/community/threads/10/like").with(asUser(7L, Role.USER))).andExpect(status().isOk());
        mockMvc.perform(post("/api/community/threads/10/bookmark").with(asUser(7L, Role.USER))).andExpect(status().isOk());
        mockMvc.perform(delete("/api/community/threads/10/bookmark").with(asUser(7L, Role.USER))).andExpect(status().isOk());

        verify(threadInteractionService).unlike(7L, 10L);
        verify(threadInteractionService).bookmark(7L, 10L);
        verify(threadInteractionService).unbookmark(7L, 10L);
    }

    @Test
    void likeBySlug_deprecatedVariantStillWorks_andNumericSlugGoesToIdVariant() throws Exception {
        when(threadInteractionService.like(7L, "eski-baslik")).thenReturn(new ThreadLikeStatusResponse(true, 1));
        when(threadInteractionService.like(7L, 555L)).thenReturn(new ThreadLikeStatusResponse(true, 1));

        mockMvc.perform(post("/api/community/threads/eski-baslik/like").with(asUser(7L, Role.USER)))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/community/threads/555/like").with(asUser(7L, Role.USER)))
                .andExpect(status().isOk());

        verify(threadInteractionService).like(7L, "eski-baslik");
        verify(threadInteractionService).like(7L, 555L);
    }

    @Test
    void commentsById_listIsPublic_createRequiresAuth_slugVariantsRemain() throws Exception {
        when(commentService.listForThread(eq(10L), eq("new"), isNull(), any(Pageable.class)))
                .thenReturn(new PageResponse<>(List.of(), 0, 20, 0, 0, true));
        when(commentService.listForThread(eq("eski-baslik"), eq("new"), isNull(), any(Pageable.class)))
                .thenReturn(new PageResponse<>(List.of(), 0, 20, 0, 0, true));

        mockMvc.perform(get("/api/community/threads/10/comments")).andExpect(status().isOk());
        mockMvc.perform(get("/api/community/threads/eski-baslik/comments")).andExpect(status().isOk());
        mockMvc.perform(post("/api/community/threads/10/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"body\":\"Yeterince uzun bir yorum metni\"}"))
                .andExpect(status().isUnauthorized());

        verify(commentService).listForThread(eq(10L), eq("new"), isNull(), any(Pageable.class));
        verify(commentService).listForThread(eq("eski-baslik"), eq("new"), isNull(), any(Pageable.class));
    }

    @Test
    void commentsById_cursorVariant_routesToIdOverload() throws Exception {
        when(commentService.listForThread(eq(10L), anyString(), isNull(), isNull(), anyInt()))
                .thenReturn(new com.example.fandoom_backend.common.dto.KeysetPageResponse<>(List.of(), false, null));

        mockMvc.perform(get("/api/community/threads/10/comments/cursor")).andExpect(status().isOk());

        verify(commentService).listForThread(eq(10L), eq("new"), isNull(), isNull(), eq(20));
    }

    // ---- POST /threads ----

    @Test
    void createThread_withoutPortalSlug_is400_withFieldError() throws Exception {
        mockMvc.perform(post("/api/community/threads").with(asUser(7L, Role.USER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"surface\":\"THEORY\",\"title\":\"Yeterince uzun başlık\",\"body\":\"gövde\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0]").value(org.hamcrest.Matchers.containsString("portalSlug")));

        verify(threadService, never()).create(anyLong(), anyBoolean(), any());
    }

    @Test
    void createThread_withPortalSlug_passesModeratorFlagFromRole() throws Exception {
        when(threadService.create(anyLong(), anyBoolean(), any(ThreadRequest.class))).thenReturn(detail());
        String body = "{\"surface\":\"THEORY\",\"title\":\"Yeterince uzun başlık\",\"portalSlug\":\"westeros\"}";

        mockMvc.perform(post("/api/community/threads").with(asUser(7L, Role.USER))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.portal.slug").value("westeros"));
        mockMvc.perform(post("/api/community/threads").with(asUser(8L, Role.MODERATOR))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());

        ArgumentCaptor<ThreadRequest> request = ArgumentCaptor.forClass(ThreadRequest.class);
        verify(threadService).create(eq(7L), eq(false), request.capture());
        assertThat(request.getValue().portalSlug()).isEqualTo("westeros");
        verify(threadService).create(eq(8L), eq(true), any(ThreadRequest.class));
    }

    @Test
    void createThread_anonymous_is401() throws Exception {
        mockMvc.perform(post("/api/community/threads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"surface\":\"THEORY\",\"title\":\"Yeterince uzun başlık\",\"portalSlug\":\"x\"}"))
                .andExpect(status().isUnauthorized());
    }

    // ---- feed ----

    @Test
    void feed_passesPortalSurfaceSortProductionAndTags() throws Exception {
        when(communityFeedService.getFeed(any(), any(), any(), any(), anyString(), any(), any(Pageable.class)))
                .thenReturn(new PageResponse<>(List.of(), 0, 20, 0, 0, true));

        mockMvc.perform(get("/api/community/feed")
                        .param("portal", "westeros").param("surface", "THEORY").param("sort", "hot")
                        .param("productionSlug", "game-of-thrones").param("tags", "dragons", "s1"))
                .andExpect(status().isOk());

        verify(communityFeedService).getFeed(eq(ThreadSurface.THEORY), eq("westeros"), eq("game-of-thrones"),
                eq(List.of("dragons", "s1")), eq("hot"), isNull(), any(Pageable.class));
    }

    @Test
    void feed_withoutParams_keepsBackwardCompatibleDefaults() throws Exception {
        when(communityFeedService.getFeed(any(), any(), any(), any(), anyString(), any(), any(Pageable.class)))
                .thenReturn(new PageResponse<>(List.of(), 0, 20, 0, 0, true));

        mockMvc.perform(get("/api/community/feed")).andExpect(status().isOk());

        verify(communityFeedService).getFeed(isNull(), isNull(), isNull(), isNull(), eq("hot"), isNull(),
                any(Pageable.class));
    }

    @Test
    void feed_unknownOrHiddenPortal_is404() throws Exception {
        when(communityFeedService.getFeed(any(), eq("gizli"), any(), any(), anyString(), any(), any(Pageable.class)))
                .thenThrow(new ResourceNotFoundException("Portal bulunamadı: slug=gizli"));

        mockMvc.perform(get("/api/community/feed").param("portal", "gizli")).andExpect(status().isNotFound());
    }

    // ---- /api/admin/community/portals ----

    private static AdminPortalResponse adminPortal() {
        return new AdminPortalResponse(1L, "yeni-portal", "Yeni", "New", null, null, null, null, null,
                PortalStatus.ACTIVE, PortalPostingPolicy.OPEN, 0, 0, 0, List.of(),
                LocalDateTime.of(2026, 9, 20, 10, 0), LocalDateTime.of(2026, 9, 20, 10, 0));
    }

    private static final String CREATE_BODY = "{\"slug\":\"yeni-portal\",\"nameTr\":\"Yeni\",\"nameEn\":\"New\"}";

    @Test
    void adminPortals_anonymous_is401_normalUserAndEditorAndModerator_are403() throws Exception {
        mockMvc.perform(get("/api/admin/community/portals")).andExpect(status().isUnauthorized());
        for (Role role : List.of(Role.USER, Role.EDITOR, Role.MODERATOR)) {
            mockMvc.perform(get("/api/admin/community/portals").with(asUser(3L, role))).andExpect(status().isForbidden());
            mockMvc.perform(post("/api/admin/community/portals").with(asUser(3L, role))
                            .contentType(MediaType.APPLICATION_JSON).content(CREATE_BODY))
                    .andExpect(status().isForbidden());
            mockMvc.perform(post("/api/admin/community/portals/yeni-portal/archive").with(asUser(3L, role)))
                    .andExpect(status().isForbidden());
        }
        verify(portalAdminService, never()).create(any());
    }

    @Test
    void adminPortals_admin_canCreateListPatchArchiveUnarchive() throws Exception {
        when(portalAdminService.create(any(PortalCreateRequest.class))).thenReturn(adminPortal());
        when(portalAdminService.list(any(Pageable.class)))
                .thenReturn(new PageResponse<>(List.of(adminPortal()), 0, 20, 1, 1, true));
        when(portalAdminService.update(eq("yeni-portal"), any())).thenReturn(adminPortal());
        when(portalAdminService.archive("yeni-portal")).thenReturn(adminPortal());
        when(portalAdminService.unarchive("yeni-portal")).thenReturn(adminPortal());

        mockMvc.perform(post("/api/admin/community/portals").with(asUser(1L, Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content(CREATE_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.slug").value("yeni-portal"));
        mockMvc.perform(get("/api/admin/community/portals").with(asUser(1L, Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].slug").value("yeni-portal"));
        mockMvc.perform(patch("/api/admin/community/portals/yeni-portal").with(asUser(1L, Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nameTr\":\"Yeni Ad\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/admin/community/portals/yeni-portal/archive").with(asUser(1L, Role.ADMIN)))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/admin/community/portals/yeni-portal/unarchive").with(asUser(1L, Role.ADMIN)))
                .andExpect(status().isOk());
    }

    @Test
    void adminPortals_create_validatesSlugFormatAndRequiredNames() throws Exception {
        mockMvc.perform(post("/api/admin/community/portals").with(asUser(1L, Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"slug\":\"Bad Slug!\",\"nameTr\":\"x\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors").isArray());

        verify(portalAdminService, never()).create(any());
    }

    @Test
    void adminPortals_noDeleteEndpoint() throws Exception {
        mockMvc.perform(delete("/api/admin/community/portals/yeni-portal").with(asUser(1L, Role.ADMIN)))
                // Not: GlobalExceptionHandler'in Exception catch-all'ı 405'i de 500'e çeviriyor (mevcut, bu işten bağımsız
                // davranış); burada önemli olan silme ucunun 2xx dönmemesi.
                .andExpect(result -> assertThat(result.getResponse().getStatus() / 100).isNotEqualTo(2));
    }
}
