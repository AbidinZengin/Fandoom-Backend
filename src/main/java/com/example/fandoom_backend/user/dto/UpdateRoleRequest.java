package com.example.fandoom_backend.user.dto;

import com.example.fandoom_backend.user.entity.Role;
import jakarta.validation.constraints.NotNull;

public record UpdateRoleRequest(@NotNull Role role) {
}
