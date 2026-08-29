package com.example.fandoom_backend.account.service;

import com.example.fandoom_backend.account.dto.CreateUserListRequest;
import com.example.fandoom_backend.account.dto.UpdateUserProfileRequest;
import com.example.fandoom_backend.common.exception.PartialUpdateValidationException;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

// PATCH /api/me/profile ve PATCH /api/me/lists/{id}, null=değişmedi kısmi
// güncelleme semantiğini korumak için controller'da @Valid UYGULAMAZ (bkz.
// AccountController) — bu yüzden gönderilen (non-null) alanlar burada elle,
// DTO'daki jakarta.validation kısıtlarıyla BİREBİR aynı kurallarla doğrulanır.
// Gönderilmeyen (null) alanlar hiç kontrol edilmez, servis katmanında da
// dokunulmadan bırakılır (bkz. UserProfileServiceImpl/UserListServiceImpl).
@Component
class PartialUpdateValidator {

    private static final Pattern ACCENT_COLOR_PATTERN = Pattern.compile("^#[0-9A-Fa-f]{6}$");

    void validateProfile(UpdateUserProfileRequest request) {
        List<String> errors = new ArrayList<>();
        checkMaxLength(errors, "bio", request.bio(), 500);
        checkMaxLength(errors, "avatarUrl", request.avatarUrl(), 500);
        checkMaxLength(errors, "bannerUrl", request.bannerUrl(), 500);
        if (request.accentColor() != null && !ACCENT_COLOR_PATTERN.matcher(request.accentColor()).matches()) {
            errors.add("accentColor: \"^#[0-9A-Fa-f]{6}$\" desenine uymalı");
        }
        throwIfNotEmpty(errors);
    }

    void validateList(CreateUserListRequest request) {
        List<String> errors = new ArrayList<>();
        if (request.title() != null) {
            if (request.title().isBlank()) {
                errors.add("title: boş olamaz");
            } else {
                checkMaxLength(errors, "title", request.title(), 150);
            }
        }
        checkMaxLength(errors, "description", request.description(), 2000);
        checkMaxLength(errors, "coverImageUrl", request.coverImageUrl(), 500);
        throwIfNotEmpty(errors);
    }

    private void checkMaxLength(List<String> errors, String field, String value, int max) {
        if (value != null && value.length() > max) {
            errors.add(field + ": en fazla " + max + " karakter olmalı");
        }
    }

    private void throwIfNotEmpty(List<String> errors) {
        if (!errors.isEmpty()) {
            throw new PartialUpdateValidationException(errors);
        }
    }
}
