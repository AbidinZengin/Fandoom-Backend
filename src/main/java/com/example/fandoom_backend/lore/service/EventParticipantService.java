package com.example.fandoom_backend.lore.service;

import com.example.fandoom_backend.lore.dto.EventParticipantRequest;
import com.example.fandoom_backend.lore.dto.EventParticipantResponse;

import java.util.List;

public interface EventParticipantService {
    List<EventParticipantResponse> listForEvent(Long eventId);
    EventParticipantResponse assign(Long eventId, EventParticipantRequest request);
    void delete(Long id);
}
