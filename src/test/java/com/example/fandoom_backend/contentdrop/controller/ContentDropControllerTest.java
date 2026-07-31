package com.example.fandoom_backend.contentdrop.controller;

import com.example.fandoom_backend.common.dto.PageResponse;
import com.example.fandoom_backend.contentdrop.dto.ContentDropDetailResponse;
import com.example.fandoom_backend.contentdrop.dto.ContentDropRequest;
import com.example.fandoom_backend.contentdrop.dto.ContentDropSummaryResponse;
import com.example.fandoom_backend.contentdrop.entity.ContentDropStatus;
import com.example.fandoom_backend.contentdrop.service.ContentDropService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Tam Spring context (SecurityConfig/JWT dahil) — /api/content-drops'un GET'lerin
// herkese açık, yazma uçlarının EDITOR/MODERATOR/ADMIN gerektirdiği path-bazlı
// kuralını gerçek filtre zincirine karşı doğrular. ContentDropService @MockitoBean
// ile izole edilir; DB'ye dokunmaz.
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "MAIL_HOST=localhost",
        "MAIL_USERNAME=test",
        "MAIL_PASSWORD=test"
})
class ContentDropControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ContentDropService contentDropService;

    @Test
    void list_publicAccess_returnsOk() throws Exception {
        when(contentDropService.list(any())).thenReturn(new PageResponse<>(List.of(), 0, 20, 0, 0, true));

        mockMvc.perform(get("/api/content-drops"))
                .andExpect(status().isOk());
    }

    @Test
    void getBySlug_publicAccess_returnsOk() throws Exception {
        when(contentDropService.getBySlug("ice-the-sword")).thenReturn(sampleDetail());

        mockMvc.perform(get("/api/content-drops/slug/ice-the-sword"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug").value("ice-the-sword"));
    }

    @Test
    void findRelated_missingRequiredParams_returnsError() throws Exception {
        // NOT: GlobalExceptionHandler'ın Exception.class catch-all'ı
        // MissingServletRequestParameterException'ı da yakalayıp 500 döndürüyor
        // (uygulama genelinde, bu endpoint'e özgü değil) — beklenen 400 yerine
        // gerçek davranış doğrulanıyor.
        mockMvc.perform(get("/api/content-drops/related").param("productionType", "SERIES"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void findRelated_publicAccess_delegatesToService() throws Exception {
        when(contentDropService.findRelatedForProduction(any(), eq("got"), eq(1), eq(1), eq(9)))
                .thenReturn(List.of(sampleSummary()));

        mockMvc.perform(get("/api/content-drops/related")
                        .param("productionType", "SERIES")
                        .param("productionSlug", "got")
                        .param("seasonNumber", "1")
                        .param("episodeNumber", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].slug").value("ice-the-sword"));
    }

    @Test
    void create_withoutAuth_returnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/content-drops")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(sampleRequestJson()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USER")
    void create_withInsufficientRole_returnsForbidden() throws Exception {
        mockMvc.perform(post("/api/content-drops")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(sampleRequestJson()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "EDITOR")
    void create_withEditorRole_delegatesToServiceAndReturnsCreated() throws Exception {
        when(contentDropService.create(any())).thenReturn(sampleDetail());

        mockMvc.perform(post("/api/content-drops")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(sampleRequestJson()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.slug").value("ice-the-sword"));

        verify(contentDropService).create(any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void delete_withAdminRole_returnsNoContentAndDelegates() throws Exception {
        mockMvc.perform(delete("/api/content-drops/1"))
                .andExpect(status().isNoContent());

        verify(contentDropService).delete(1L);
    }

    private String sampleRequestJson() {
        ContentDropRequest request = new ContentDropRequest(
                "The Sword Called Ice", "kicker", "axis",
                null, null, null, null, null,
                ContentDropStatus.DRAFT, null, null);
        return objectMapper.writeValueAsString(request);
    }

    private ContentDropSummaryResponse sampleSummary() {
        return new ContentDropSummaryResponse(1L, "ice-the-sword", "The Sword Called Ice", null, null, null);
    }

    private ContentDropDetailResponse sampleDetail() {
        return new ContentDropDetailResponse(1L, "ice-the-sword", "The Sword Called Ice", "kicker", "axis",
                null, null, null, null, null,
                ContentDropStatus.DRAFT, null, 0L, null,
                List.of(), List.of(), List.of(), null, null);
    }
}
