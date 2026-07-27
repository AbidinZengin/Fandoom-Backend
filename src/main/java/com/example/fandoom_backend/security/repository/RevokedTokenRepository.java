package com.example.fandoom_backend.security.repository;

import com.example.fandoom_backend.security.entity.RevokedToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface RevokedTokenRepository extends JpaRepository<RevokedToken, Long> {
    boolean existsByJti(String jti);
    long deleteByExpiresAtBefore(LocalDateTime cutoff);
}
