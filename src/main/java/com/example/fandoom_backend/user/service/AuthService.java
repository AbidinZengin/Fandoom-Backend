package com.example.fandoom_backend.user.service;

import com.example.fandoom_backend.user.dto.AuthResponse;
import com.example.fandoom_backend.user.dto.LoginRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface AuthService {
    AuthResponse login(LoginRequest request, HttpServletResponse response);
    AuthResponse refresh(HttpServletRequest request, HttpServletResponse response);
    void logout(HttpServletRequest request, HttpServletResponse response);
}
