package com.example.fandoom_backend.security;

import com.example.fandoom_backend.user.entity.Role;
import com.example.fandoom_backend.user.entity.User;
import com.example.fandoom_backend.user.security.CustomUserDetails;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

// Elle yazılan hata yanıtları (401 giriş yok, 403 yetki yok, iki 429 hız sınırı) servlet'in varsayılan kodlaması
// ISO-8859-1 olduğundan ğ/ş/ı harflerini '?' yapıyordu (harf KAYBI, yalnız yanlış görünüm değil). Bu test başlığın
// UTF-8 bildirdiğini ve Türkçe karakterlerin gövdede bozulmadan durduğunu sabitler. Spring converter'larından geçen
// hatalar (400/404/409) zaten UTF-8'dir, bu test yalnız elle yazılan dört yolu kapsar.
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "MAIL_HOST=localhost",
        "MAIL_USERNAME=test",
        "MAIL_PASSWORD=test"
})
class ErrorResponseEncodingTest {

    @Autowired
    private MockMvc mockMvc;

    private static void assertUtf8(MockHttpServletResponse response, String expectedText) throws Exception {
        assertThat(response.getContentType()).containsIgnoringCase("charset=UTF-8");
        assertThat(response.getContentAsString(StandardCharsets.UTF_8)).contains(expectedText);
    }

    @Test
    void unauthorized401_keepsTurkishCharacters() throws Exception {
        MockHttpServletResponse response = mockMvc.perform(get("/api/me/profile")).andReturn().getResponse();

        assertThat(response.getStatus()).isEqualTo(401);
        assertUtf8(response, "Kimlik doğrulama gerekli: lütfen giriş yapın");
    }

    @Test
    void forbidden403_keepsTurkishCharacters() throws Exception {
        MockHttpServletResponse response = mockMvc.perform(get("/api/users/5").with(asUser(7L, Role.USER)))
                .andReturn().getResponse();

        assertThat(response.getStatus()).isEqualTo(403);
        assertUtf8(response, "Bu işlem için yetkiniz yok");
    }

    @Test
    void authRateLimit429_keepsTurkishCharacters() throws Exception {
        // Kendi IP'si: AuthRateLimitFilter IP başına 10/dk sayar, aynı context'i paylaşan diğer testlerin
        // (127.0.0.1) kovasını tüketmesin.
        MockHttpServletResponse last = null;
        for (int i = 0; i < 11; i++) {
            last = mockMvc.perform(post("/api/auth/login")
                            .with(request -> {
                                request.setRemoteAddr("10.9.9.9");
                                return request;
                            })
                            .contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andReturn().getResponse();
        }

        assertThat(last.getStatus()).isEqualTo(429);
        assertUtf8(last, "Çok fazla istek gönderildi, lütfen bir dakika sonra tekrar deneyin");
    }

    @Test
    void communityRateLimit429_keepsTurkishCharacters() throws Exception {
        // Bu teste özel userId: CommunityRateLimitFilter userId başına sayar. Gövde {} -> ilk 10 istek doğrulamada 400
        // ile biter (servise inmez), 11.'yi filtre 429 ile keser.
        MockHttpServletResponse last = null;
        for (int i = 0; i < 11; i++) {
            last = mockMvc.perform(post("/api/community/threads").with(asUser(9_001L, Role.USER))
                            .contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andReturn().getResponse();
        }

        assertThat(last.getStatus()).isEqualTo(429);
        assertUtf8(last, "Çok fazla istek gönderildi, lütfen bir süre sonra tekrar deneyin");
    }

    private RequestPostProcessor asUser(Long id, Role role) {
        User user = User.builder().id(id).username("kullanici" + id).password("x").role(role)
                .emailVerified(true).build();
        CustomUserDetails principal = new CustomUserDetails(user);
        return SecurityMockMvcRequestPostProcessors.authentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }
}
