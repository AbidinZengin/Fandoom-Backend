package com.example.fandoom_backend.community;

import com.example.fandoom_backend.common.exception.ApiErrorResponse;
import com.example.fandoom_backend.user.security.CustomUserDetails;
import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Basit, tek-instance in-memory rate limiter — {@code security/AuthRateLimitFilter}
 * ile aynı desen (ConcurrentHashMap + zaman penceresi), ama IP değil userId
 * bazlı (thread/yorum spam'ini önlemek için — brute-force login'den farklı bir
 * tehdit modeli). {@code JwtAuthenticationFilter}'dan SONRA çalışır ki
 * {@code SecurityContextHolder}'da authentication zaten çözülmüş olsun; anonim
 * istekte userId çözülemeyeceği için bu filtre hiçbir şey yapmadan devam eder —
 * o istek zaten yetkilendirme aşamasında 401'e düşer.
 */
@Component
@RequiredArgsConstructor
public class CommunityRateLimitFilter extends OncePerRequestFilter {

    private static final String THREAD_CREATE_PATH = "/api/community/threads";
    private static final String COMMENT_CREATE_PATTERN = "/api/community/threads/*/comments";
    // Merkezi/polimorfik yorum ucu (THREAD/BLOG/SEASON/EPISODE) — aynı
    // comment-create bucket'ını paylaşır, tam yol eşleşmesi yeterli (Ant
    // pattern gerekmiyor).
    private static final String CENTRAL_COMMENT_CREATE_PATH = "/api/community/comments";
    // Doğrudan video yükleme imzası (media/): sıradan kullanıcıya açık tek media ucu, kötüye kullanımı sınırla.
    private static final String VIDEO_SIGNATURE_PATH = "/api/media/videos/signature";
    private static final int VIDEO_SIGNATURE_LIMIT = 20;
    private static final int THREAD_CREATE_LIMIT = 10;
    private static final int COMMENT_CREATE_LIMIT = 30;
    private static final long WINDOW_MILLIS = 3_600_000L; // 1 saat

    private final ObjectMapper objectMapper;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return resolveRule(request) == null;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {
        LimitRule rule = resolveRule(request);
        Long userId = rule == null ? null : currentUserId();
        if (rule == null || userId == null) {
            filterChain.doFilter(request, response);
            return;
        }

        String key = userId + ":" + rule.bucket();
        long now = Instant.now().toEpochMilli();
        Window window = windows.compute(key, (k, existing) -> {
            if (existing == null || now - existing.windowStart() > WINDOW_MILLIS) {
                return new Window(now, 1);
            }
            return new Window(existing.windowStart(), existing.count() + 1);
        });

        if (window.count() > rule.limit()) {
            ApiErrorResponse body = ApiErrorResponse.of(
                    HttpStatus.TOO_MANY_REQUESTS.value(), HttpStatus.TOO_MANY_REQUESTS.getReasonPhrase(),
                    "Çok fazla istek gönderildi, lütfen bir süre sonra tekrar deneyin", request.getRequestURI());
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            objectMapper.writeValue(response.getWriter(), body);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private LimitRule resolveRule(HttpServletRequest request) {
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return null;
        }
        String uri = request.getRequestURI();
        if (THREAD_CREATE_PATH.equals(uri)) {
            return new LimitRule("thread-create", THREAD_CREATE_LIMIT);
        }
        if (VIDEO_SIGNATURE_PATH.equals(uri)) {
            return new LimitRule("video-signature", VIDEO_SIGNATURE_LIMIT);
        }
        if (CENTRAL_COMMENT_CREATE_PATH.equals(uri) || pathMatcher.match(COMMENT_CREATE_PATTERN, uri)) {
            return new LimitRule("comment-create", COMMENT_CREATE_LIMIT);
        }
        return null;
    }

    private Long currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails principal)) {
            return null;
        }
        return principal.getId();
    }

    private record LimitRule(String bucket, int limit) {
    }

    private record Window(long windowStart, int count) {
    }
}
