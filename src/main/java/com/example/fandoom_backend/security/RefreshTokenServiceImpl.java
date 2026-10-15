package com.example.fandoom_backend.security;

import com.example.fandoom_backend.security.entity.RefreshToken;
import com.example.fandoom_backend.security.repository.RefreshTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final long refreshExpirationMs;

    // JwtService ile aynı desen: @Value alan için @RequiredArgsConstructor kullanılamıyor, elle constructor.
    public RefreshTokenServiceImpl(RefreshTokenRepository refreshTokenRepository,
                                    @Value("${jwt.refresh-expiration-ms}") long refreshExpirationMs) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.refreshExpirationMs = refreshExpirationMs;
    }

    @Override
    @Transactional
    public String createToken(Long userId) {
        String token = UUID.randomUUID().toString();
        refreshTokenRepository.save(RefreshToken.builder()
                .token(token)
                .userId(userId)
                .expiryDate(LocalDateTime.now().plus(refreshExpirationMs, ChronoUnit.MILLIS))
                .revoked(false)
                .build());
        return token;
    }

    @Override
    @Transactional(readOnly = true)
    public Long validateAndGetUserId(String token) {
        RefreshToken stored = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new InvalidRefreshTokenException("Geçersiz refresh token"));
        if (stored.isRevoked()) {
            throw new InvalidRefreshTokenException("Refresh token iptal edilmiş");
        }
        if (stored.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new InvalidRefreshTokenException("Refresh token süresi dolmuş");
        }
        return stored.getUserId();
    }

    @Override
    @Transactional
    public void revoke(String token) {
        refreshTokenRepository.revokeByToken(token);
    }
}
