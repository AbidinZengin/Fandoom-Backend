package com.example.fandoom_backend.account.controller;

import com.example.fandoom_backend.account.dto.CreateUserListRequest;
import com.example.fandoom_backend.account.dto.LikeStatusResponse;
import com.example.fandoom_backend.account.entity.SavedItemType;
import com.example.fandoom_backend.account.service.UserFollowService;
import com.example.fandoom_backend.account.service.UserLikeService;
import com.example.fandoom_backend.account.service.UserListService;
import com.example.fandoom_backend.account.service.UserSavedItemService;
import com.example.fandoom_backend.community.dto.ProfileStats;
import com.example.fandoom_backend.community.dto.UpdateUserProfileRequest;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import tools.jackson.databind.ObjectMapper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Tam Spring context (SecurityConfig/JWT dahil) — /api/me/**'nin hicbir path
// kuraline girmeyip otomatik anyRequest().authenticated()'a dustugunu (rol
// kisiti YOK, herhangi bir rolle login yeterli) ve @AuthenticationPrincipal
// CustomUserDetails'ten userId'nin dogru cozuldugunu dogrular. Servisler
// @MockitoBean ile izole edilir; DB'ye dokunmaz. @WithMockUser kullanilamiyor
// çünkü principal tipi CustomUserDetails olmali (varsayilan
// org.springframework.security.core.userdetails.User degil) — bunun yerine
// gercek bir CustomUserDetails'i authentication() post-processor'iyla enjekte
// ediyoruz (bkz. asUser()).
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "MAIL_HOST=localhost",
        "MAIL_USERNAME=test",
        "MAIL_PASSWORD=test"
})
class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserProfileService userProfileService;

    @MockitoBean
    private UserListService userListService;

    @MockitoBean
    private UserSavedItemService userSavedItemService;

    @MockitoBean
    private UserLikeService userLikeService;

    @MockitoBean
    private UserFollowService userFollowService;

    @Test
    void getProfile_withoutAuth_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/me/profile"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getProfile_authenticatedAsPlainUser_resolvesPrincipalIdAndReturnsOk() throws Exception {
        when(userProfileService.getProfile(7L, 7L)).thenReturn(sampleProfile());

        mockMvc.perform(get("/api/me/profile").with(asUser(7L, "abidin", Role.USER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("abidin"));

        verify(userProfileService).getProfile(7L, 7L);
    }

    // /api/me/** hiçbir path kuralına girmiyor (SecurityConfig'te ayrı bir
    // satır yok) — otomatik anyRequest().authenticated()'a düşüyor, yani rol
    // kısıtı olmadan herhangi bir rolle login yeterli. ADMIN de USER de aynı
    // şekilde erişebilmeli.
    @Test
    void getProfile_authenticatedAsAdmin_alsoReturnsOk_noRoleRestriction() throws Exception {
        when(userProfileService.getProfile(9L, 9L)).thenReturn(sampleProfile());

        mockMvc.perform(get("/api/me/profile").with(asUser(9L, "root", Role.ADMIN)))
                .andExpect(status().isOk());

        verify(userProfileService).getProfile(9L, 9L);
    }

    @Test
    void updateProfile_authenticated_delegatesWithPrincipalIdAndBody() throws Exception {
        when(userProfileService.updateProfile(eq(7L), any())).thenReturn(sampleProfile());
        UpdateUserProfileRequest request = new UpdateUserProfileRequest("yeni bio", null, null, null, null);

        mockMvc.perform(patch("/api/me/profile").with(asUser(7L, "abidin", Role.USER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(userProfileService).updateProfile(7L, request);
    }

    // @Valid + DTO'daki @Pattern, PartialUpdateValidator kaldırıldıktan sonra
    // geçersiz accentColor'ı controller katmanında (servise hiç girmeden) reddeder.
    @Test
    void updateProfile_invalidAccentColor_returnsBadRequestWithoutCallingService() throws Exception {
        UpdateUserProfileRequest request = new UpdateUserProfileRequest(null, null, null, "not-a-color", null);

        mockMvc.perform(patch("/api/me/profile").with(asUser(7L, "abidin", Role.USER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(userProfileService, never()).updateProfile(any(), any());
    }

    // @Validated(OnUpdate.class): title gönderilip boş/boşluk bırakılırsa
    // (null'dan farklı olarak) reddedilir — OnCreate.class'taki @NotBlank yerine
    // OnUpdate.class'taki @Pattern(".*\S.*") bunu sağlar (bkz. CreateUserListRequest).
    @Test
    void updateList_blankTitle_returnsBadRequestWithoutCallingService() throws Exception {
        CreateUserListRequest request = new CreateUserListRequest("   ", null, null, null);

        mockMvc.perform(patch("/api/me/lists/1").with(asUser(7L, "abidin", Role.USER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(userListService, never()).update(any(), any(), any());
    }

    @Test
    void like_authenticated_delegatesWithPrincipalIdAndPathVariables() throws Exception {
        when(userLikeService.like(7L, SavedItemType.MOVIE, 42L))
                .thenReturn(new LikeStatusResponse(true, 3L));

        mockMvc.perform(post("/api/me/likes/MOVIE/42").with(asUser(7L, "abidin", Role.USER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.liked").value(true))
                .andExpect(jsonPath("$.likeCount").value(3));

        verify(userLikeService).like(7L, SavedItemType.MOVIE, 42L);
    }

    @Test
    void likes_withoutAuth_returnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/me/likes/MOVIE/42"))
                .andExpect(status().isUnauthorized());
    }

    private RequestPostProcessor asUser(Long id, String username, Role role) {
        User user = User.builder().id(id).username(username).password("x").role(role).emailVerified(true).build();
        CustomUserDetails principal = new CustomUserDetails(user);
        return SecurityMockMvcRequestPostProcessors.authentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    private UserProfileResponse sampleProfile() {
        return new UserProfileResponse(7L, "abidin", "bio", null, null, null, false,
                new ProfileStats(0, 0, 0, 0, null), 0L, 0L, false);
    }
}
