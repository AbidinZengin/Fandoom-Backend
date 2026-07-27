package com.example.fandoom_backend.common.exception;

public class PremiumRequiredException extends RuntimeException {
    public PremiumRequiredException(String message) {
        super(message);
    }
}
