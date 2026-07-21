package com.example.fandoom_backend.person.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record PersonDetailResponse(
        Long id, String name, String slug, String bio, String photoUrl, LocalDate birthDate,
        LocalDateTime createdAt, LocalDateTime updatedAt) {
}
