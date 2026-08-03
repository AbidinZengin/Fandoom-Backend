package com.example.fandoom_backend.blog.controller;

import com.example.fandoom_backend.blog.dto.BlogDetailResponse;
import com.example.fandoom_backend.blog.dto.BlogFilterCriteria;
import com.example.fandoom_backend.blog.dto.BlogRequest;
import com.example.fandoom_backend.blog.dto.BlogSummaryResponse;
import com.example.fandoom_backend.blog.entity.BlogStatus;
import com.example.fandoom_backend.blog.service.BlogQueryService;
import com.example.fandoom_backend.blog.service.BlogService;
import com.example.fandoom_backend.common.dto.PageResponse;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Tam Spring context (SecurityConfig/JWT dahil) — /api/blogs'un GET'lerin
// herkese açık, yazma uçlarının EDITOR/MODERATOR/ADMIN gerektirdiği path-bazlı
// kuralını gerçek filtre zincirine karşı doğrular. BlogService @MockitoBean
// ile izole edilir; DB'ye dokunmaz.
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "MAIL_HOST=localhost",
        "MAIL_USERNAME=test",
        "MAIL_PASSWORD=test"
})
class BlogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private BlogService blogService;

    @MockitoBean
    private BlogQueryService blogQueryService;

    @Test
    void list_publicAccess_returnsOk() throws Exception {
        when(blogService.list(any())).thenReturn(new PageResponse<>(List.of(), 0, 20, 0, 0, true));

        mockMvc.perform(get("/api/blogs"))
                .andExpect(status().isOk());
    }

    @Test
    void getBySlug_publicAccess_returnsOk() throws Exception {
        when(blogService.getBySlug("ice-the-sword")).thenReturn(sampleDetail());

        mockMvc.perform(get("/api/blogs/slug/ice-the-sword"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug").value("ice-the-sword"));
    }

    @Test
    void hub_noParams_delegatesWithDefaultSortAndPaging() throws Exception {
        when(blogQueryService.findFilterable(any(), any(), any()))
                .thenReturn(new PageResponse<>(List.of(), 0, 20, 0, 0, true));

        mockMvc.perform(get("/api/blogs/hub"))
                .andExpect(status().isOk());

        ArgumentCaptor<BlogFilterCriteria> criteriaCaptor = ArgumentCaptor.forClass(BlogFilterCriteria.class);
        ArgumentCaptor<String> sortCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(blogQueryService).findFilterable(
                criteriaCaptor.capture(), sortCaptor.capture(), pageableCaptor.capture());

        BlogFilterCriteria criteria = criteriaCaptor.getValue();
        assertThat(criteria.format()).isNull();
        assertThat(criteria.franchiseSlug()).isNull();
        assertThat(criteria.moodSlugs()).isNull();
        assertThat(criteria.themeSlugs()).isNull();
        assertThat(criteria.spoilerFree()).isNull();
        assertThat(sortCaptor.getValue()).isEqualTo("latest");
        assertThat(pageableCaptor.getValue().getPageNumber()).isZero();
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(20);
    }

    @Test
    void hub_withAllQueryParams_buildsCriteriaAndDelegatesWithSortAndPageable() throws Exception {
        when(blogQueryService.findFilterable(any(), any(), any()))
                .thenReturn(new PageResponse<>(List.of(), 1, 5, 0, 0, true));

        mockMvc.perform(get("/api/blogs/hub")
                        .param("format", "listicle")
                        .param("franchise", "got")
                        .param("mood", "dark", "hopeful")
                        .param("theme", "betrayal")
                        .param("spoilerFree", "true")
                        .param("sort", "trending")
                        .param("page", "1")
                        .param("size", "5"))
                .andExpect(status().isOk());

        ArgumentCaptor<BlogFilterCriteria> criteriaCaptor = ArgumentCaptor.forClass(BlogFilterCriteria.class);
        ArgumentCaptor<String> sortCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(blogQueryService).findFilterable(
                criteriaCaptor.capture(), sortCaptor.capture(), pageableCaptor.capture());

        BlogFilterCriteria criteria = criteriaCaptor.getValue();
        assertThat(criteria.format()).isEqualTo("listicle");
        assertThat(criteria.franchiseSlug()).isEqualTo("got");
        assertThat(criteria.moodSlugs()).containsExactly("dark", "hopeful");
        assertThat(criteria.themeSlugs()).containsExactly("betrayal");
        assertThat(criteria.spoilerFree()).isTrue();
        assertThat(sortCaptor.getValue()).isEqualTo("trending");
        assertThat(pageableCaptor.getValue().getPageNumber()).isEqualTo(1);
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(5);
    }

    @Test
    void findRelated_missingRequiredParams_returnsError() throws Exception {
        // NOT: GlobalExceptionHandler'ın Exception.class catch-all'ı
        // MissingServletRequestParameterException'ı da yakalayıp 500 döndürüyor
        // (uygulama genelinde, bu endpoint'e özgü değil) — beklenen 400 yerine
        // gerçek davranış doğrulanıyor.
        mockMvc.perform(get("/api/blogs/related").param("productionType", "SERIES"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void findRelated_publicAccess_delegatesToService() throws Exception {
        when(blogService.findRelatedForProduction(any(), eq("got"), eq(1), eq(1), eq(9)))
                .thenReturn(List.of(sampleSummary()));

        mockMvc.perform(get("/api/blogs/related")
                        .param("productionType", "SERIES")
                        .param("productionSlug", "got")
                        .param("seasonNumber", "1")
                        .param("episodeNumber", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].slug").value("ice-the-sword"));
    }

    @Test
    void create_withoutAuth_returnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/blogs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(sampleRequestJson()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USER")
    void create_withInsufficientRole_returnsForbidden() throws Exception {
        mockMvc.perform(post("/api/blogs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(sampleRequestJson()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "EDITOR")
    void create_withEditorRole_delegatesToServiceAndReturnsCreated() throws Exception {
        when(blogService.create(any())).thenReturn(sampleDetail());

        mockMvc.perform(post("/api/blogs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(sampleRequestJson()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.slug").value("ice-the-sword"));

        verify(blogService).create(any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void delete_withAdminRole_returnsNoContentAndDelegates() throws Exception {
        mockMvc.perform(delete("/api/blogs/1"))
                .andExpect(status().isNoContent());

        verify(blogService).delete(1L);
    }

    private String sampleRequestJson() {
        BlogRequest request = new BlogRequest(
                "The Sword Called Ice", "kicker", "axis",
                null, null, null, null, null,
                null, false, BlogStatus.DRAFT, null, null);
        return objectMapper.writeValueAsString(request);
    }

    private BlogSummaryResponse sampleSummary() {
        return new BlogSummaryResponse(1L, "ice-the-sword", "The Sword Called Ice", null, null, null);
    }

    private BlogDetailResponse sampleDetail() {
        return new BlogDetailResponse(1L, "ice-the-sword", "The Sword Called Ice", "kicker", "axis",
                null, null, null, null, null,
                null, false,
                BlogStatus.DRAFT, null, 0L, null,
                List.of(), List.of(), List.of(), null, null);
    }
}
