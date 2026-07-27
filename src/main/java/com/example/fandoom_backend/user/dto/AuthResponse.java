package com.example.fandoom_backend.user.dto;

import com.example.fandoom_backend.user.entity.Role;

public record AuthResponse(String username, Role role, String token) {
}
