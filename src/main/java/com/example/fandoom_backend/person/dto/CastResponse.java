package com.example.fandoom_backend.person.dto;

public record CastResponse(
        Long id,
        PersonSummaryResponse person,
        CharacterResponse character,
        Integer billingOrder) {
}
