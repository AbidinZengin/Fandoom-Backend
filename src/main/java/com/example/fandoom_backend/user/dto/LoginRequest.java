package com.example.fandoom_backend.user.dto;

import jakarta.validation.constraints.NotBlank;

// usernameOrEmail: kullanıcı adı ya da e-posta kabul edilir (bkz.
// CustomUserDetailsService — önce username, bulunamazsa email denenir).
public record LoginRequest(@NotBlank String usernameOrEmail, @NotBlank String password) {
}
