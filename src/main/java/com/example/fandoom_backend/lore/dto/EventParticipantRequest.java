package com.example.fandoom_backend.lore.dto;

import com.example.fandoom_backend.lore.entity.ParticipantType;
import jakarta.validation.constraints.NotNull;

public record EventParticipantRequest(
        @NotNull ParticipantType participantType,
        @NotNull Long participantId) {
}
