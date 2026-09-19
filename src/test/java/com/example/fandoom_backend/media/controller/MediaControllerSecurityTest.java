package com.example.fandoom_backend.media.controller;

import com.example.fandoom_backend.media.dto.VideoUploadSignatureResponse;
import com.example.fandoom_backend.media.service.VideoUploadSigner;
import com.example.fandoom_backend.user.entity.Role;
import com.example.fandoom_backend.user.entity.User;
import com.example.fandoom_backend.user.security.CustomUserDetails;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Gerçek SecurityConfig + filtre zinciri (BlogControllerTest deseni): imza ucu sıradan USER'a açık, anonime kapalı,
// kullanıcı başına rate limit'li; diğer /api/media/** uçları hâlâ EDITOR+ (kural genişlemedi).
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {"MAIL_HOST=localhost", "MAIL_USERNAME=test", "MAIL_PASSWORD=test"})
class MediaControllerSecurityTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private VideoUploadSigner videoUploadSigner;

    private void stubSignature() {
        when(videoUploadSigner.createSignature()).thenReturn(new VideoUploadSignatureResponse(
                "https://api.cloudinary.com/v1_1/demo/video/upload", "key", "sig",
                Map.of("tags", "pending"), 52_428_800L, 120));
    }

    private static CustomUserDetails principal(long id, Role role) {
        return new CustomUserDetails(User.builder().id(id).username("u" + id).email("u" + id + "@x.com")
                .password("x").role(role).build());
    }

    @Test
    void signature_anonymous_returns401() throws Exception {
        mockMvc.perform(post("/api/media/videos/signature")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USER")
    void signature_plainUser_returnsParams() throws Exception {
        stubSignature();

        mockMvc.perform(post("/api/media/videos/signature"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.signature").value("sig"))
                .andExpect(jsonPath("$.params.tags").value("pending"))
                .andExpect(jsonPath("$.maxDurationSeconds").value(120));
    }

    @Test
    @WithMockUser(roles = "EDITOR")
    void signature_editor_returnsParams() throws Exception {
        stubSignature();

        mockMvc.perform(post("/api/media/videos/signature")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "USER")
    void otherMediaEndpoints_stillRequireEditorRole() throws Exception {
        mockMvc.perform(multipart("/api/media/videos")
                        .file(new MockMultipartFile("file", "a.mp4", "video/mp4", new byte[]{1})))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/media/videos").param("url", "x")).andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/media/images").param("url", "x")).andExpect(status().isForbidden());
    }

    @Test
    void signature_isRateLimitedPerUser_20PerHour() throws Exception {
        stubSignature();
        // Diğer testlerle çakışmasın diye bu teste özgü kullanıcı id'si
        var user = user(principal(987_654L, Role.USER));

        for (int i = 0; i < 20; i++) {
            mockMvc.perform(post("/api/media/videos/signature").with(user)).andExpect(status().isOk());
        }
        mockMvc.perform(post("/api/media/videos/signature").with(user)).andExpect(status().isTooManyRequests());

        // Başka kullanıcı etkilenmez
        mockMvc.perform(post("/api/media/videos/signature").with(user(principal(987_655L, Role.USER))))
                .andExpect(status().isOk());
    }
}
