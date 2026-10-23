package com.example.fandoom_backend.account.controller;

import com.example.fandoom_backend.community.dto.ProfileStats;
import com.example.fandoom_backend.community.dto.UserProfileResponse;
import com.example.fandoom_backend.community.service.UserProfileService;
import com.example.fandoom_backend.user.entity.Role;
import com.example.fandoom_backend.user.entity.User;
import com.example.fandoom_backend.user.security.CustomUserDetails;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// POST /api/me/profile/avatar|banner: gerçek SecurityConfig + hız sınırı filtresi, servis mock'lu.
// Kanıtlar: sıradan USER yükleyebilir (/api/media/** ise EDITOR+ kalır), girişsiz 401, userId JWT'den gelir,
// avatar+banner AYNI saatlik kovayı paylaşır (10), hatalı istekler 5xx değil 4xx.
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "MAIL_HOST=localhost",
        "MAIL_USERNAME=test",
        "MAIL_PASSWORD=test"
})
class ProfileImageUploadEndpointTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserProfileService userProfileService;

    private static MockMultipartFile png() {
        return new MockMultipartFile("file", "a.png", "image/png", new byte[]{1, 2, 3});
    }

    @Test
    void withoutAuth_isUnauthorizedAndServiceUntouched() throws Exception {
        mockMvc.perform(multipart("/api/me/profile/avatar").file(png())).andExpect(status().isUnauthorized());
        mockMvc.perform(multipart("/api/me/profile/banner").file(png())).andExpect(status().isUnauthorized());

        verifyNoInteractions(userProfileService);
    }

    @Test
    void plainUser_canUploadAvatar_andPrincipalIdIsUsed() throws Exception {
        when(userProfileService.uploadAvatar(eq(7L), any())).thenReturn(sample("https://new/avatar.png"));

        mockMvc.perform(multipart("/api/me/profile/avatar").file(png()).with(asUser(7L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avatarUrl").value("https://new/avatar.png"));

        verify(userProfileService).uploadAvatar(eq(7L), any());
    }

    @Test
    void plainUser_canUploadBanner() throws Exception {
        when(userProfileService.uploadBanner(eq(7L), any())).thenReturn(sample(null));

        mockMvc.perform(multipart("/api/me/profile/banner").file(png()).with(asUser(7L)))
                .andExpect(status().isOk());

        verify(userProfileService).uploadBanner(eq(7L), any());
    }

    @Test
    void mediaEndpointsStayClosedToPlainUsers() throws Exception {
        // Avatar için /api/media/** USER'a AÇILMADI (serbest klasör + URL ile silme riski).
        mockMvc.perform(multipart("/api/media/images").file(png()).with(asUser(7L)))
                .andExpect(status().isForbidden());
    }

    @Test
    void missingFilePart_isBadRequestWithMessage_notInternalServerError() throws Exception {
        mockMvc.perform(multipart("/api/me/profile/avatar").with(asUser(7L)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Zorunlu dosya eksik: file"));
    }

    @Test
    void nonMultipartBody_isUnsupportedMediaType() throws Exception {
        mockMvc.perform(post("/api/me/profile/avatar").with(asUser(7L))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    void avatarAndBannerShareOneHourlyBucketOfTen() throws Exception {
        // Bu teste özel userId: filtre userId başına sayar.
        for (int i = 0; i < 10; i++) {
            String path = i % 2 == 0 ? "/api/me/profile/avatar" : "/api/me/profile/banner";
            mockMvc.perform(multipart(path).file(png()).with(asUser(9_002L))).andExpect(status().isOk());
        }

        mockMvc.perform(multipart("/api/me/profile/avatar").file(png()).with(asUser(9_002L)))
                .andExpect(status().isTooManyRequests());
        mockMvc.perform(multipart("/api/me/profile/banner").file(png()).with(asUser(9_002L)))
                .andExpect(status().isTooManyRequests());
    }

    private UserProfileResponse sample(String avatarUrl) {
        return new UserProfileResponse(7L, "abidin", "bio", avatarUrl, null, null, false,
                new ProfileStats(0, 0, 0, 0, null), 0L, 0L, false, false, true, false, false, null);
    }

    private RequestPostProcessor asUser(Long id) {
        User user = User.builder().id(id).username("kullanici" + id).password("x").role(Role.USER)
                .emailVerified(true).build();
        CustomUserDetails principal = new CustomUserDetails(user);
        return SecurityMockMvcRequestPostProcessors.authentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }
}
