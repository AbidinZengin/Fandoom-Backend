package com.example.fandoom_backend.user.service;

import com.example.fandoom_backend.security.JwtService;
import com.example.fandoom_backend.security.RevokedTokenService;
import com.example.fandoom_backend.user.dto.AuthResponse;
import com.example.fandoom_backend.user.dto.LoginRequest;
import com.example.fandoom_backend.user.exception.EmailNotVerifiedException;
import com.example.fandoom_backend.user.security.CustomUserDetails;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final String BEARER_PREFIX = "Bearer ";

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RevokedTokenService revokedTokenService;

    @Override
    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password()));
        CustomUserDetails principal = (CustomUserDetails) authentication.getPrincipal();
        if (!principal.isEmailVerified()) {
            throw new EmailNotVerifiedException("E-posta adresiniz henüz doğrulanmadı, lütfen gelen kutunuzu kontrol edin.");
        }
        String token = jwtService.generateToken(principal);
        return new AuthResponse(principal.getUsername(), principal.getRole(), token);
    }

    @Override
    @Transactional
    public void logout(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            return;
        }
        String token = header.substring(BEARER_PREFIX.length());
        try {
            String jti = jwtService.getJti(token);
            LocalDateTime expiresAt = jwtService.getExpiration(token).toInstant()
                    .atZone(ZoneId.systemDefault()).toLocalDateTime();
            revokedTokenService.revoke(jti, expiresAt);
        } catch (JwtException e) {
            // Token zaten geçersiz/süresi dolmuş — iptal edilecek bir şey yok, logout idempotent no-op.
        }
    }
}
