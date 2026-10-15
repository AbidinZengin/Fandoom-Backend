package com.example.fandoom_backend.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

// Cookie oluşturma/temizleme mantığı tek yerde toplanır (AuthServiceImpl login'de set eder,
// logout'ta temizler) — ikisinin aynı isim/path/secure/sameSite ayarlarından sürüklenmesini önler.
@Component
public class RefreshTokenCookieFactory {

    public static final String COOKIE_NAME = "refresh_token";
    private static final String COOKIE_PATH = "/api/auth";

    private final boolean secure;
    private final long maxAgeSeconds;

    public RefreshTokenCookieFactory(@Value("${jwt.refresh-cookie-secure}") boolean secure,
                                      @Value("${jwt.refresh-expiration-ms}") long refreshExpirationMs) {
        this.secure = secure;
        this.maxAgeSeconds = refreshExpirationMs / 1000;
    }

    public ResponseCookie create(String token) {
        return build(token, maxAgeSeconds);
    }

    // MaxAge=0: tarayıcıya cookie'yi hemen silmesini söyler (logout).
    public ResponseCookie clear() {
        return build("", 0);
    }

    private ResponseCookie build(String value, long maxAgeSeconds) {
        return ResponseCookie.from(COOKIE_NAME, value)
                .httpOnly(true)
                .secure(secure)
                .path(COOKIE_PATH)
                .sameSite("Lax")
                .maxAge(maxAgeSeconds)
                .build();
    }
}
