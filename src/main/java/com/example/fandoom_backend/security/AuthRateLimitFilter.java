package com.example.fandoom_backend.security;

import com.example.fandoom_backend.common.exception.ApiErrorResponse;
import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Basit, tek-instance in-memory rate limiter. Yatay ölçeklemede (birden fazla
 * uygulama instance'ı) her instance kendi sayacını tutacağı için etkin limit
 * instance sayısıyla çarpılır ve restart'ta sıfırlanır — bilinçli bir trade-off,
 * proje henüz tek instance çalıştığı ve Redis'e aşina olunmadığı için ertelendi.
 */
@Component
@RequiredArgsConstructor
public class AuthRateLimitFilter extends OncePerRequestFilter {

    private static final Set<String> LIMITED_PATHS = Set.of(
            "/api/auth/login", "/api/auth/register", "/api/auth/resend-verification");
    private static final int MAX_REQUESTS_PER_WINDOW = 10;
    private static final long WINDOW_MILLIS = 60_000L;

    private final ObjectMapper objectMapper;
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !LIMITED_PATHS.contains(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {
        String ip = request.getRemoteAddr();
        long now = Instant.now().toEpochMilli();

        Window window = windows.compute(ip, (key, existing) -> {
            if (existing == null || now - existing.windowStart() > WINDOW_MILLIS) {
                return new Window(now, 1);
            }
            return new Window(existing.windowStart(), existing.count() + 1);
        });

        if (window.count() > MAX_REQUESTS_PER_WINDOW) {
            ApiErrorResponse body = ApiErrorResponse.of(
                    HttpStatus.TOO_MANY_REQUESTS.value(), HttpStatus.TOO_MANY_REQUESTS.getReasonPhrase(),
                    "Çok fazla istek gönderildi, lütfen bir dakika sonra tekrar deneyin", request.getRequestURI());
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            objectMapper.writeValue(response.getWriter(), body);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private record Window(long windowStart, int count) {
    }
}
