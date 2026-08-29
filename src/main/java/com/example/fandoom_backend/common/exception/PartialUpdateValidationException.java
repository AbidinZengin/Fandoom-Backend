package com.example.fandoom_backend.common.exception;

import java.util.List;

/**
 * Kısmi (PATCH) güncelleme uçlarında, {@code null = değişmedi} semantiği
 * korunduğu için {@code @Valid} uygulanamayan (tüm alanları zorunlu kılardı)
 * durumlarda, servis katmanının SADECE gönderilen (non-null) alanları elle
 * doğrulayıp topladığı hataları taşır. {@code TranslationIncompleteException}
 * ile aynı desen (bkz. blog/BlogServiceImpl.validateMirror) — modüle özel bir
 * exception yerine, "manuel kısmi doğrulama" jenerik olduğu için common/'da,
 * herhangi bir modül PATCH'te aynı ihtiyacı duyduğunda yeniden kullanılabilir.
 */
public class PartialUpdateValidationException extends RuntimeException {

    private final List<String> fieldErrors;

    public PartialUpdateValidationException(List<String> fieldErrors) {
        super("Doğrulama hatası");
        this.fieldErrors = fieldErrors;
    }

    public List<String> getFieldErrors() {
        return fieldErrors;
    }
}
