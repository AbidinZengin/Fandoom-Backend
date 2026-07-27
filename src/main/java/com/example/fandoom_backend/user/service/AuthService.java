package com.example.fandoom_backend.user.service;

import com.example.fandoom_backend.user.dto.AuthResponse;
import com.example.fandoom_backend.user.dto.LoginRequest;
import jakarta.servlet.http.HttpServletRequest;

public interface AuthService {
    AuthResponse login(LoginRequest request);
    void logout(HttpServletRequest request);
}
