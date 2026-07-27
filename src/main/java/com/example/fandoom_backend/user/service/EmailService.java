package com.example.fandoom_backend.user.service;

public interface EmailService {
    void sendVerificationEmail(String to, String username, String verificationLink);
}
