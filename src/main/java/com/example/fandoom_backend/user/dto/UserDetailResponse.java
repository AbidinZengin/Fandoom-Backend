package com.example.fandoom_backend.user.dto;

import com.example.fandoom_backend.user.entity.Role;

import java.time.LocalDateTime;

public record UserDetailResponse(
        Long id,
        String username,
        String email,
        Role role,
        boolean emailVerified,
        LocalDateTime premiumExpiresAt,
        boolean premiumActive,
        LocalDateTime createdAt) {
}
