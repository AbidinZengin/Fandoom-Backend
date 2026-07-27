package com.example.fandoom_backend.security;

import com.example.fandoom_backend.security.entity.RevokedToken;
import com.example.fandoom_backend.security.repository.RevokedTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class RevokedTokenServiceImpl implements RevokedTokenService {

    private final RevokedTokenRepository revokedTokenRepository;

    @Override
    @Transactional
    public void revoke(String jti, LocalDateTime expiresAt) {
        revokedTokenRepository.save(RevokedToken.builder().jti(jti).expiresAt(expiresAt).build());
    }
}
