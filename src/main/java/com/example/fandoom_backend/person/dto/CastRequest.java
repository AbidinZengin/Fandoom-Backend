package com.example.fandoom_backend.person.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CastRequest(
        @NotNull Long personId,
        @NotNull Long characterId,
        @Positive Integer billingOrder) {
}
