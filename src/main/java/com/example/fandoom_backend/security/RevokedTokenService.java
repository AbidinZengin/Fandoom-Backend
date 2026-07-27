package com.example.fandoom_backend.security;

import java.time.LocalDateTime;

public interface RevokedTokenService {

    void revoke(String jti, LocalDateTime expiresAt);
}
