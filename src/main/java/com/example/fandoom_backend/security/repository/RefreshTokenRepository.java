package com.example.fandoom_backend.security.repository;

import com.example.fandoom_backend.security.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    // In-memory entity'yi bilerek set etmiyoruz (Thread sayaç deseniyle aynı gerekçe): tek satırlık
    // atomik UPDATE, dirty-checking'in eşzamanlı bir başka revoke'u ezmesini önler.
    // clearAutomatically=true ŞART: aksi halde aynı transaction içinde revoke'u hemen izleyen bir
    // validateAndGetUserId (findByToken) çağrısı, persistence context'in 1. seviye cache'indeki
    // bayat (revoked=false) entity'yi döner — bulk update DB'yi günceller ama session'ı senkronize etmez.
    @Modifying(clearAutomatically = true)
    @Query("UPDATE RefreshToken r SET r.revoked = true WHERE r.token = :token")
    int revokeByToken(@Param("token") String token);

    long deleteByExpiryDateBefore(LocalDateTime cutoff);
}
