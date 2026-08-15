package com.example.fandoom_backend.common.exception;

import java.util.List;

/**
 * PUBLISHED geçişinde "tam ayna" kuralı (K1) ihlal edildiğinde fırlatılır:
 * bir çevrilebilir alan yalnızca bir dilde dolu, diğerinde boş.
 */
public class TranslationIncompleteException extends RuntimeException {

    private final List<String> fieldErrors;

    public TranslationIncompleteException(List<String> fieldErrors) {
        super("Yayınlamadan önce tüm alanlar iki dilde de dolu olmalı");
        this.fieldErrors = fieldErrors;
    }

    public List<String> getFieldErrors() {
        return fieldErrors;
    }
}
