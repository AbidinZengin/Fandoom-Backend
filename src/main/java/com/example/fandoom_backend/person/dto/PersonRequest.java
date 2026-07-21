package com.example.fandoom_backend.person.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record PersonRequest(
        @NotBlank @Size(max = 150) String name,
        @Size(max = 5000) String bio,
        @Size(max = 500) String photoUrl,
        LocalDate birthDate) {
}
