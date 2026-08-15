package com.example.fandoom_backend.lore.dto;

import com.example.fandoom_backend.lore.entity.ParticipantType;

public record EventParticipantResponse(
        Long id, Long eventId, ParticipantType participantType, Long participantId) {
}
