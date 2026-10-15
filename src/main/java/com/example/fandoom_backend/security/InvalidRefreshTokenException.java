package com.example.fandoom_backend.security;

import org.springframework.security.core.AuthenticationException;

// AuthenticationException'ı extend eder: AuthExceptionHandler zaten bu tipi 401'e çeviriyor,
// refresh akışı için ayrı bir exception handler'a gerek kalmıyor.
public class InvalidRefreshTokenException extends AuthenticationException {

    public InvalidRefreshTokenException(String message) {
        super(message);
    }
}
