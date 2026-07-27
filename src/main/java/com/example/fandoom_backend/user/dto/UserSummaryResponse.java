package com.example.fandoom_backend.user.dto;

import com.example.fandoom_backend.user.entity.Role;

public record UserSummaryResponse(Long id, String username, Role role, boolean premiumActive) {
}
