package com.example.fandoom_backend.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;

/**
 * {@code @Order(LOWEST_PRECEDENCE)}: Spring, bir exception için advice bean'ler arasında
 * en spesifik eşleşmeyi aramaz, sıraya göre ilk uygun bean'i kullanır. Bu handler'ın
 * {@code Exception.class} catch-all'ı en son değerlendirilmezse, diğer modüllerin
 * (ör. {@code user.exception.AuthExceptionHandler}) daha spesifik handler'larını
 * gölgeleyebilir.
 */
@RestControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE)
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler(InvalidReferenceException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidReference(InvalidReferenceException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ApiErrorResponse> handleDuplicate(DuplicateResourceException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), request);
    }

    // Spring Security'nin kendi AccessDeniedException'ı — burada, servis
    // katmanında path-bazlı yetkilendirmenin ifade edemediği ince kontroller
    // (ör. Community'de "sahip veya moderatör") için fırlatılır. Bu handler
    // olmadan @ExceptionHandler(Exception.class) catch-all'ı bunu 500'e
    // düşürürdü — RestAccessDeniedHandler yalnızca ExceptionTranslationFilter'a
    // (yani path-bazlı authorizeHttpRequests reddine) kadar ulaşan, DispatcherServlet
    // içinde bir @ExceptionHandler tarafından hiç yakalanmamış exception'ları görür.
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN, "Bu işlem için yetkiniz yok", request);
    }

    @ExceptionHandler(InvalidFileException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidFile(InvalidFileException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    @ExceptionHandler(TranslationIncompleteException.class)
    public ResponseEntity<ApiErrorResponse> handleTranslationIncomplete(TranslationIncompleteException ex, HttpServletRequest request) {
        ApiErrorResponse body = ApiErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(),
                ex.getMessage(), request.getRequestURI(), ex.getFieldErrors());
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, "Veri bütünlüğü ihlali (muhtemelen benzersizlik kısıtı)", request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<String> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .toList();
        ApiErrorResponse body = ApiErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "Doğrulama hatası", request.getRequestURI(), fieldErrors);
        return ResponseEntity.badRequest().body(body);
    }

    // @RequestParam zorunlu bir query parametresi (ör. subjectType/subjectId)
    // hiç gönderilmezse fırlar. Bu handler olmadan Exception.class catch-all'ı
    // bunu 500'e düşürürdü — client hatası (400) olması gerekirdi.
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiErrorResponse> handleMissingParam(
            MissingServletRequestParameterException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "Zorunlu parametre eksik: " + ex.getParameterName(), request);
    }

    // @RequestParam'a tipiyle uyuşmayan bir değer gelirse (ör. subjectId=abc
    // ya da subjectId=undefined — Long'a çevrilemez; subjectType=blog —
    // enum'a çevrilemez, büyük/küçük harf duyarlı) fırlar. Aynı gerekçeyle
    // 400'e çevrilir, 500'e düşmesine izin verilmez.
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST,
                "Geçersiz parametre değeri: " + ex.getName() + "=" + ex.getValue(), request);
    }

    // Bozuk JSON gövdesi / bilinmeyen enum değeri / uyuşmayan alan tipi: client hatası (400).
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleUnreadableBody(
            HttpMessageNotReadableException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "İstek gövdesi okunamadı veya geçersiz", request);
    }

    // Spring'in kendi client-hatası exception'ları (405 metod yok, 415 desteklenmeyen içerik tipi, 406, 404 kaynak yok)
    // ErrorResponse arayüzünü taşır: durum kodu ve başlıklar (ör. 405'te Allow) exception'ın kendisinden gelir. Bu handler
    // olmadan catch-all bunları 500 + ERROR stacktrace'e düşürürdü (permitAll GET uçlarına anonim istekle log şişirilebilirdi).
    @ExceptionHandler({HttpRequestMethodNotSupportedException.class, HttpMediaTypeNotSupportedException.class,
            HttpMediaTypeNotAcceptableException.class, NoResourceFoundException.class})
    public ResponseEntity<ApiErrorResponse> handleFrameworkClientError(Exception ex, HttpServletRequest request) {
        ErrorResponse errorResponse = (ErrorResponse) ex;
        HttpStatus status = HttpStatus.valueOf(errorResponse.getStatusCode().value());
        String message = switch (status) {
            case NOT_FOUND -> "Kaynak bulunamadı";
            case METHOD_NOT_ALLOWED -> "Bu yol için HTTP metodu desteklenmiyor";
            case UNSUPPORTED_MEDIA_TYPE -> "Desteklenmeyen içerik tipi";
            default -> "İstek işlenemedi";
        };
        ApiErrorResponse body = ApiErrorResponse.of(status.value(), status.getReasonPhrase(), message, request.getRequestURI());
        return ResponseEntity.status(status).headers(errorResponse.getHeaders()).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Beklenmeyen hata: {} {}", request.getMethod(), request.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Beklenmeyen bir hata oluştu", request);
    }

    private ResponseEntity<ApiErrorResponse> build(HttpStatus status, String message, HttpServletRequest request) {
        ApiErrorResponse body = ApiErrorResponse.of(status.value(), status.getReasonPhrase(), message, request.getRequestURI());
        return ResponseEntity.status(status).body(body);
    }
}
