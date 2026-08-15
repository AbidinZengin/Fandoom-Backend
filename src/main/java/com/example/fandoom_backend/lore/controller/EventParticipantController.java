package com.example.fandoom_backend.lore.controller;

import com.example.fandoom_backend.lore.dto.EventParticipantRequest;
import com.example.fandoom_backend.lore.dto.EventParticipantResponse;
import com.example.fandoom_backend.lore.service.EventParticipantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/lore/events")
@RequiredArgsConstructor
public class EventParticipantController {

    private final EventParticipantService eventParticipantService;

    @GetMapping("/{eventId}/participants")
    public List<EventParticipantResponse> listForEvent(@PathVariable Long eventId) {
        return eventParticipantService.listForEvent(eventId);
    }

    @PostMapping("/{eventId}/participants")
    @ResponseStatus(HttpStatus.CREATED)
    public EventParticipantResponse assign(@PathVariable Long eventId,
                                            @Valid @RequestBody EventParticipantRequest request) {
        return eventParticipantService.assign(eventId, request);
    }

    @DeleteMapping("/participants/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        eventParticipantService.delete(id);
    }
}
