package com.example.fandoom_backend.common.exception;

import java.time.LocalDateTime;
import java.util.List;

public record ApiErrorResponse(
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        String path,
        List<String> fieldErrors) {

    public static ApiErrorResponse of(int status, String error, String message, String path) {
        return new ApiErrorResponse(LocalDateTime.now(), status, error, message, path, null);
    }

    public static ApiErrorResponse of(int status, String error, String message, String path, List<String> fieldErrors) {
        return new ApiErrorResponse(LocalDateTime.now(), status, error, message, path, fieldErrors);
    }
}
