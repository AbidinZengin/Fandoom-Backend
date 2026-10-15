package com.example.fandoom_backend.user.service;

import com.example.fandoom_backend.security.InvalidRefreshTokenException;
import com.example.fandoom_backend.security.JwtService;
import com.example.fandoom_backend.security.RefreshTokenCookieFactory;
import com.example.fandoom_backend.security.RefreshTokenService;
import com.example.fandoom_backend.security.RevokedTokenService;
import com.example.fandoom_backend.user.dto.AuthResponse;
import com.example.fandoom_backend.user.dto.LoginRequest;
import com.example.fandoom_backend.user.entity.User;
import com.example.fandoom_backend.user.exception.EmailNotVerifiedException;
import com.example.fandoom_backend.user.repository.UserRepository;
import com.example.fandoom_backend.user.security.CustomUserDetails;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final String BEARER_PREFIX = "Bearer ";

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RevokedTokenService revokedTokenService;
    private final RefreshTokenService refreshTokenService;
    private final RefreshTokenCookieFactory refreshTokenCookieFactory;
    private final UserRepository userRepository;

    @Override
    public AuthResponse login(LoginRequest request, HttpServletResponse response) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.usernameOrEmail(), request.password()));
        CustomUserDetails principal = (CustomUserDetails) authentication.getPrincipal();
        if (!principal.isEmailVerified()) {
            throw new EmailNotVerifiedException("E-posta adresiniz henüz doğrulanmadı, lütfen gelen kutunuzu kontrol edin.");
        }
        String accessToken = jwtService.generateToken(principal);
        String refreshToken = refreshTokenService.createToken(principal.getId());
        response.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookieFactory.create(refreshToken).toString());
        return new AuthResponse(principal.getUsername(), principal.getRole(), accessToken);
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse refresh(HttpServletRequest request, HttpServletResponse response) {
        String rawToken = extractRefreshCookie(request)
                .orElseThrow(() -> new InvalidRefreshTokenException("Refresh token bulunamadı"));
        Long userId = refreshTokenService.validateAndGetUserId(rawToken);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new InvalidRefreshTokenException("Kullanıcı bulunamadı"));
        CustomUserDetails principal = new CustomUserDetails(user);
        String accessToken = jwtService.generateToken(principal);
        return new AuthResponse(principal.getUsername(), principal.getRole(), accessToken);
    }

    @Override
    @Transactional
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        revokeAccessToken(request);
        extractRefreshCookie(request).ifPresent(refreshTokenService::revoke);
        response.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookieFactory.clear().toString());
    }

    private void revokeAccessToken(HttpServletRequest request) {
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

    private Optional<String> extractRefreshCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }
        return Arrays.stream(cookies)
                .filter(cookie -> RefreshTokenCookieFactory.COOKIE_NAME.equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst();
    }
}
