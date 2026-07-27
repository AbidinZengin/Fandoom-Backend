package com.example.fandoom_backend.security;

import com.example.fandoom_backend.security.repository.RevokedTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Süresi zaten dolmuş JWT'ler {@link JwtAuthenticationFilter} tarafından kendi
 * exp claim'i üzerinden zaten reddedildiği için bu temizlik doğruluk için değil,
 * sadece revoked_token tablosunun büyümesini önlemek (hijyen) içindir.
 */
@Component
@RequiredArgsConstructor
public class RevokedTokenCleanupScheduler {

    private final RevokedTokenRepository revokedTokenRepository;

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void purgeExpired() {
        revokedTokenRepository.deleteByExpiresAtBefore(LocalDateTime.now());
    }
}
