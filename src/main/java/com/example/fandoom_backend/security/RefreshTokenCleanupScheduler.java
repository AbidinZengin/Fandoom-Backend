package com.example.fandoom_backend.security;

import com.example.fandoom_backend.security.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * RevokedTokenCleanupScheduler ile aynı gerekçe: süresi dolmuş refresh token'lar
 * {@link RefreshTokenServiceImpl#validateAndGetUserId} tarafından zaten reddediliyor,
 * bu temizlik doğruluk için değil yalnızca refresh_token tablosunun büyümesini
 * önlemek (hijyen) içindir.
 */
@Component
@RequiredArgsConstructor
public class RefreshTokenCleanupScheduler {

    private final RefreshTokenRepository refreshTokenRepository;

    @Scheduled(cron = "0 30 3 * * *")
    @Transactional
    public void purgeExpired() {
        refreshTokenRepository.deleteByExpiryDateBefore(LocalDateTime.now());
    }
}
