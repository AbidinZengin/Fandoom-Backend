package com.example.fandoom_backend.user.controller;

import com.example.fandoom_backend.security.RefreshTokenCookieFactory;
import com.example.fandoom_backend.user.entity.Role;
import com.example.fandoom_backend.user.entity.User;
import com.example.fandoom_backend.user.repository.UserRepository;
import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Login -> Refresh -> Logout uçtan uca akışı gerçek SecurityConfig + gerçek DB'ye karşı
// (@Transactional: test sonunda rollback). Register akışını (e-posta gönderimi) bilerek
// atlar: kullanıcı doğrudan emailVerified=true olarak persist edilir.
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {"MAIL_HOST=localhost", "MAIL_USERNAME=test", "MAIL_PASSWORD=test"})
@Transactional
class AuthFlowIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private static final String RAW_PASSWORD = "sifre-12345";

    private User persistVerifiedUser(String username) {
        return userRepository.save(User.builder()
                .username(username).email(username + "@example.com")
                .password(passwordEncoder.encode(RAW_PASSWORD))
                .role(Role.USER).emailVerified(true).build());
    }

    @Test
    void login_setsRefreshCookie_andReturnsAccessToken() throws Exception {
        persistVerifiedUser("auth-flow-login");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"usernameOrEmail":"auth-flow-login","password":"%s"}""".formatted(RAW_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.username").value("auth-flow-login"))
                .andExpect(result -> {
                    Cookie cookie = result.getResponse().getCookie(RefreshTokenCookieFactory.COOKIE_NAME);
                    assertThat(cookie).isNotNull();
                    assertThat(cookie.isHttpOnly()).isTrue();
                    assertThat(cookie.getPath()).isEqualTo("/api/auth");
                    assertThat(cookie.getMaxAge()).isGreaterThan(0);
                    assertThat(cookie.getValue()).isNotBlank();
                });
    }

    @Test
    void refresh_withValidCookie_returnsNewAccessToken() throws Exception {
        persistVerifiedUser("auth-flow-refresh");

        var loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"usernameOrEmail":"auth-flow-refresh","password":"%s"}""".formatted(RAW_PASSWORD)))
                .andExpect(status().isOk())
                .andReturn();
        Cookie refreshCookie = loginResult.getResponse().getCookie(RefreshTokenCookieFactory.COOKIE_NAME);

        mockMvc.perform(post("/api/auth/refresh").cookie(refreshCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.username").value("auth-flow-refresh"));
    }

    @Test
    void refresh_withoutCookie_returnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/auth/refresh"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refresh_withBogusCookie_returnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(new Cookie(RefreshTokenCookieFactory.COOKIE_NAME, "not-a-real-token")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logout_revokesRefreshToken_andClearsCookie() throws Exception {
        persistVerifiedUser("auth-flow-logout");

        var loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"usernameOrEmail":"auth-flow-logout","password":"%s"}""".formatted(RAW_PASSWORD)))
                .andExpect(status().isOk())
                .andReturn();
        Cookie refreshCookie = loginResult.getResponse().getCookie(RefreshTokenCookieFactory.COOKIE_NAME);
        String accessToken = JsonPath.read(loginResult.getResponse().getContentAsString(), "$.token");

        var logoutResult = mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + accessToken)
                        .cookie(refreshCookie))
                .andExpect(status().isNoContent())
                .andReturn();
        Cookie clearedCookie = logoutResult.getResponse().getCookie(RefreshTokenCookieFactory.COOKIE_NAME);
        assertThat(clearedCookie).isNotNull();
        assertThat(clearedCookie.getMaxAge()).isZero();

        // logout sonrası aynı refresh token artık revoked -> 401
        mockMvc.perform(post("/api/auth/refresh").cookie(refreshCookie))
                .andExpect(status().isUnauthorized());
    }
}
