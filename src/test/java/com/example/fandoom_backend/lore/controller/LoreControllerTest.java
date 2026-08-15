package com.example.fandoom_backend.lore.controller;

import com.example.fandoom_backend.lore.dto.EventParticipantResponse;
import com.example.fandoom_backend.lore.dto.EventResponse;
import com.example.fandoom_backend.lore.dto.GroupAssignmentResponse;
import com.example.fandoom_backend.lore.dto.GroupResponse;
import com.example.fandoom_backend.lore.dto.LocationResponse;
import com.example.fandoom_backend.lore.dto.TaxonomyCategoryRequest;
import com.example.fandoom_backend.lore.dto.TaxonomyCategoryResponse;
import com.example.fandoom_backend.lore.entity.ParticipantType;
import com.example.fandoom_backend.lore.entity.SubjectType;
import com.example.fandoom_backend.lore.entity.TaggableType;
import com.example.fandoom_backend.lore.service.EventParticipantService;
import com.example.fandoom_backend.lore.service.EventService;
import com.example.fandoom_backend.lore.service.GroupAssignmentService;
import com.example.fandoom_backend.lore.service.GroupService;
import com.example.fandoom_backend.lore.service.LocationService;
import com.example.fandoom_backend.lore.service.TaxonomyCategoryService;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Tam Spring context (SecurityConfig/JWT dahil) — /api/lore/**'in hem nested
// (/api/movies|series/{id}/lore/...) hem kök (/api/lore/...) path'lerde
// GET'lerin herkese açık, yazma uçlarının EDITOR/MODERATOR/ADMIN gerektirdiği
// kuralını gerçek filtre zincirine karşı doğrular (GroupControllerTest'teki
// desenin devamı — grup group/ -> lore/ taşındığı için buraya taşındı).
// Servisler @MockitoBean ile izole edilir, DB'ye dokunmaz.
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "MAIL_HOST=localhost",
        "MAIL_USERNAME=test",
        "MAIL_PASSWORD=test"
})
class LoreControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TaxonomyCategoryService taxonomyCategoryService;
    @MockitoBean
    private GroupService groupService;
    @MockitoBean
    private GroupAssignmentService groupAssignmentService;
    @MockitoBean
    private LocationService locationService;
    @MockitoBean
    private EventService eventService;
    @MockitoBean
    private EventParticipantService eventParticipantService;

    @Test
    void listCategoriesForSeries_publicAccess_returnsOk() throws Exception {
        when(taxonomyCategoryService.listForSeries(10L)).thenReturn(List.of(
                new TaxonomyCategoryResponse(1L, "Haneler", "haneler", SubjectType.SERIES, 10L)));

        mockMvc.perform(get("/api/series/10/lore/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].slug").value("haneler"));
    }

    @Test
    void addCategoryToSeries_withoutAuth_returnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/series/10/lore/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TaxonomyCategoryRequest("Haneler"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USER")
    void addCategoryToSeries_withInsufficientRole_returnsForbidden() throws Exception {
        mockMvc.perform(post("/api/series/10/lore/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TaxonomyCategoryRequest("Haneler"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "EDITOR")
    void addCategoryToSeries_withEditorRole_delegatesAndReturnsCreated() throws Exception {
        when(taxonomyCategoryService.addToSeries(any(), any())).thenReturn(
                new TaxonomyCategoryResponse(1L, "Haneler", "haneler", SubjectType.SERIES, 10L));

        mockMvc.perform(post("/api/series/10/lore/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TaxonomyCategoryRequest("Haneler"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.slug").value("haneler"));
    }

    @Test
    void getGroupById_publicAccess_returnsOk() throws Exception {
        when(groupService.getById(1L)).thenReturn(new GroupResponse(1L, "House Stark", "house-stark", null,
                1L, "Haneler", "haneler", SubjectType.SERIES, 10L, null));

        mockMvc.perform(get("/api/lore/groups/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug").value("house-stark"));
    }

    @Test
    @WithMockUser(roles = "MODERATOR")
    void updateGroup_withModeratorRole_delegatesAndReturnsOk() throws Exception {
        when(groupService.update(any(), any())).thenReturn(new GroupResponse(1L, "House Stark", "house-stark", null,
                1L, "Haneler", "haneler", SubjectType.SERIES, 10L, null));

        mockMvc.perform(put("/api/lore/groups/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"House Stark\",\"categoryId\":1}"))
                .andExpect(status().isOk());
    }

    @Test
    void listAssignmentsForCharacter_publicAccess_returnsOk() throws Exception {
        when(groupAssignmentService.listForTarget(TaggableType.CHARACTER, 42L)).thenReturn(List.of(
                new GroupAssignmentResponse(1L, 1L, "House Stark", "house-stark", TaggableType.CHARACTER, 42L)));

        mockMvc.perform(get("/api/lore/groups/assignments?taggableType=CHARACTER&taggableId=42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].taggableId").value(42));
    }

    @Test
    void listLocationsForSeries_publicAccess_returnsOk() throws Exception {
        when(locationService.listForSeries(10L)).thenReturn(List.of(
                new LocationResponse(1L, "Winterfell", "winterfell", null, null, null, SubjectType.SERIES, 10L)));

        mockMvc.perform(get("/api/series/10/lore/locations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].slug").value("winterfell"));
    }

    @Test
    void listEventsForSeries_publicAccess_returnsOk() throws Exception {
        when(eventService.listForSeries(10L)).thenReturn(List.of(
                new EventResponse(1L, "Red Wedding", null, 1, null, SubjectType.SERIES, 10L, null, null, null,
                        false, null)));

        mockMvc.perform(get("/api/series/10/lore/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].orderIndex").value(1));
    }

    @Test
    void addEventToSeries_withoutAuth_returnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/series/10/lore/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Red Wedding\",\"orderIndex\":1}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listParticipantsForEvent_publicAccess_returnsOk() throws Exception {
        when(eventParticipantService.listForEvent(1L)).thenReturn(List.of(
                new EventParticipantResponse(1L, 1L, ParticipantType.CHARACTER, 42L)));

        mockMvc.perform(get("/api/lore/events/1/participants"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].participantId").value(42));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void assignParticipant_withAdminRole_delegatesAndReturnsCreated() throws Exception {
        when(eventParticipantService.assign(any(), any())).thenReturn(
                new EventParticipantResponse(1L, 1L, ParticipantType.GROUP, 7L));

        mockMvc.perform(post("/api/lore/events/1/participants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"participantType\":\"GROUP\",\"participantId\":7}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.participantId").value(7));
    }
}
