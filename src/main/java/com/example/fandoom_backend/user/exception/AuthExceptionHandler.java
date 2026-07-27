package com.example.fandoom_backend.user.exception;

import com.example.fandoom_backend.common.exception.ApiErrorResponse;
import com.example.fandoom_backend.common.exception.PremiumRequiredException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * {@code @Order(HIGHEST_PRECEDENCE)}: bu handler'lar, {@link com.example.fandoom_backend.common.exception.GlobalExceptionHandler}'ın
 * genel {@code Exception.class} catch-all'ından önce değerlendirilmeli — aksi halde Spring'in
 * advice bean'leri arasında en spesifik eşleşmeyi değil, sıradaki ilk uygun bean'i seçmesi
 * yüzünden (ör.) {@link com.example.fandoom_backend.user.service.AuthServiceImpl#login} içindeki
 * {@code EmailNotVerifiedException} 403 yerine genel 500 olarak dönebilir.
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AuthExceptionHandler {

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiErrorResponse> handleAuthentication(AuthenticationException ex, HttpServletRequest request) {
        return build(HttpStatus.UNAUTHORIZED, "Kimlik doğrulama gerekli: lütfen giriş yapın", request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN, "Bu işlem için yetkiniz yok", request);
    }

    @ExceptionHandler(PremiumRequiredException.class)
    public ResponseEntity<ApiErrorResponse> handlePremiumRequired(PremiumRequiredException ex, HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN, ex.getMessage(), request);
    }

    @ExceptionHandler(EmailNotVerifiedException.class)
    public ResponseEntity<ApiErrorResponse> handleEmailNotVerified(EmailNotVerifiedException ex, HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN, ex.getMessage(), request);
    }

    private ResponseEntity<ApiErrorResponse> build(HttpStatus status, String message, HttpServletRequest request) {
        ApiErrorResponse body = ApiErrorResponse.of(status.value(), status.getReasonPhrase(), message, request.getRequestURI());
        return ResponseEntity.status(status).body(body);
    }
}
