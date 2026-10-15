package com.example.fandoom_backend.security;

public interface RefreshTokenService {

    // Rastgele (JWT DEĞİL) bir refresh token üretir, DB'ye kaydeder ve ham token'ı döner.
    String createToken(Long userId);

    // Token'ı bulur; yok/revoked/süresi dolmuşsa InvalidRefreshTokenException (401) fırlatır.
    // Entity'yi (RefreshToken) DEĞİL sahibinin userId'sini döner — security/entity kendi modülünün
    // dışına (ör. user/) sızmasın diye (cross-module kuralı, bkz. CLAUDE.md).
    Long validateAndGetUserId(String token);

    // Idempotent: token yok/zaten revoked ise no-op.
    void revoke(String token);
}
