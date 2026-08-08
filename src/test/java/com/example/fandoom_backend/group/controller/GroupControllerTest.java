package com.example.fandoom_backend.group.controller;

import com.example.fandoom_backend.group.dto.GroupAssignmentResponse;
import com.example.fandoom_backend.group.dto.GroupRequest;
import com.example.fandoom_backend.group.dto.GroupResponse;
import com.example.fandoom_backend.group.entity.GroupType;
import com.example.fandoom_backend.group.entity.SubjectType;
import com.example.fandoom_backend.group.entity.TaggableType;
import com.example.fandoom_backend.group.service.GroupAssignmentService;
import com.example.fandoom_backend.group.service.GroupService;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Tam Spring context (SecurityConfig/JWT dahil) — /api/groups'un GET'lerin
// herkese açık, yazma uçlarının EDITOR/MODERATOR/ADMIN gerektirdiği path-bazlı
// kuralını gerçek filtre zincirine karşı doğrular (BlogControllerTest'teki
// desenin aynısı). GroupService/GroupAssignmentService @MockitoBean ile
// izole edilir; DB'ye dokunmaz.
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "MAIL_HOST=localhost",
        "MAIL_USERNAME=test",
        "MAIL_PASSWORD=test"
})
class GroupControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private GroupService groupService;

    @MockitoBean
    private GroupAssignmentService groupAssignmentService;

    @Test
    void listForMovie_publicAccess_returnsOk() throws Exception {
        when(groupService.listForMovie(1L, null)).thenReturn(List.of(sampleResponse()));

        mockMvc.perform(get("/api/movies/1/groups"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].slug").value("house-stark"));
    }

    @Test
    void addToMovie_withoutAuth_returnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/movies/1/groups")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(sampleRequestJson()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USER")
    void addToMovie_withInsufficientRole_returnsForbidden() throws Exception {
        mockMvc.perform(post("/api/movies/1/groups")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(sampleRequestJson()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "EDITOR")
    void addToMovie_withEditorRole_delegatesToServiceAndReturnsCreated() throws Exception {
        when(groupService.addToMovie(any(), any())).thenReturn(sampleResponse());

        mockMvc.perform(post("/api/movies/1/groups")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(sampleRequestJson()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.slug").value("house-stark"));

        verify(groupService).addToMovie(any(), any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void delete_withAdminRole_returnsNoContentAndDelegates() throws Exception {
        mockMvc.perform(delete("/api/groups/1"))
                .andExpect(status().isNoContent());

        verify(groupService).delete(1L);
    }

    @Test
    void assign_withoutAuth_returnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/groups/1/assignments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(sampleAssignmentRequestJson()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "MODERATOR")
    void assign_withModeratorRole_delegatesToServiceAndReturnsCreated() throws Exception {
        when(groupAssignmentService.assign(any(), any())).thenReturn(
                new GroupAssignmentResponse(1L, 1L, "House Stark", "house-stark",
                        GroupType.FACTION, TaggableType.CHARACTER, 42L));

        mockMvc.perform(post("/api/groups/1/assignments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(sampleAssignmentRequestJson()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.taggableId").value(42));

        verify(groupAssignmentService).assign(any(), any());
    }

    private String sampleRequestJson() {
        return objectMapper.writeValueAsString(new GroupRequest("House Stark", null, GroupType.FACTION));
    }

    private String sampleAssignmentRequestJson() {
        return "{\"taggableType\":\"CHARACTER\",\"taggableId\":42}";
    }

    private GroupResponse sampleResponse() {
        return new GroupResponse(1L, "House Stark", "house-stark", null, GroupType.FACTION, SubjectType.MOVIE, 1L);
    }
}
