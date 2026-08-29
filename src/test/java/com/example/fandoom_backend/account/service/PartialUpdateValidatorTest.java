package com.example.fandoom_backend.account.service;

import com.example.fandoom_backend.account.dto.CreateUserListRequest;
import com.example.fandoom_backend.account.dto.UpdateUserProfileRequest;
import com.example.fandoom_backend.common.exception.PartialUpdateValidationException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// PATCH /api/me/profile ve PATCH /api/me/lists/{id}, null=değişmedi semantiğini
// korumak için @Valid uygulamıyor — bu davranışı sağlayan PartialUpdateValidator'ın
// gönderilen (non-null) alanları DTO kısıtlarıyla (Size/Pattern/NotBlank) birebir
// aynı kurallarla doğruladığını, gönderilmeyen (null) alanları hiç sorgulamadığını
// doğrudan test eder.
class PartialUpdateValidatorTest {

    private final PartialUpdateValidator validator = new PartialUpdateValidator();

    @Test
    void validateProfile_allFieldsNull_passes() {
        assertThatCode(() -> validator.validateProfile(
                new UpdateUserProfileRequest(null, null, null, null, null)))
                .doesNotThrowAnyException();
    }

    @Test
    void validateProfile_validPartialFields_passes() {
        assertThatCode(() -> validator.validateProfile(
                new UpdateUserProfileRequest("kısa bio", null, null, "#ABCDEF", true)))
                .doesNotThrowAnyException();
    }

    @Test
    void validateProfile_bioTooLong_throws() {
        String tooLong = "a".repeat(501);
        assertThatThrownBy(() -> validator.validateProfile(
                new UpdateUserProfileRequest(tooLong, null, null, null, null)))
                .isInstanceOf(PartialUpdateValidationException.class);
    }

    @Test
    void validateProfile_accentColorDoesNotMatchPattern_throws() {
        assertThatThrownBy(() -> validator.validateProfile(
                new UpdateUserProfileRequest(null, null, null, "blue", null)))
                .isInstanceOf(PartialUpdateValidationException.class);
    }

    @Test
    void validateProfile_avatarUrlTooLong_throws() {
        String tooLong = "https://example.com/" + "a".repeat(500);
        assertThatThrownBy(() -> validator.validateProfile(
                new UpdateUserProfileRequest(null, tooLong, null, null, null)))
                .isInstanceOf(PartialUpdateValidationException.class);
    }

    @Test
    void validateList_allFieldsNull_passes() {
        assertThatCode(() -> validator.validateList(
                new CreateUserListRequest(null, null, null, null)))
                .doesNotThrowAnyException();
    }

    @Test
    void validateList_validPartialTitle_passes() {
        assertThatCode(() -> validator.validateList(
                new CreateUserListRequest("Yeni Başlık", null, null, null)))
                .doesNotThrowAnyException();
    }

    @Test
    void validateList_blankTitle_throws() {
        assertThatThrownBy(() -> validator.validateList(
                new CreateUserListRequest("   ", null, null, null)))
                .isInstanceOf(PartialUpdateValidationException.class);
    }

    @Test
    void validateList_titleTooLong_throws() {
        String tooLong = "a".repeat(151);
        assertThatThrownBy(() -> validator.validateList(
                new CreateUserListRequest(tooLong, null, null, null)))
                .isInstanceOf(PartialUpdateValidationException.class);
    }

    @Test
    void validateList_descriptionTooLong_throws() {
        String tooLong = "a".repeat(2001);
        assertThatThrownBy(() -> validator.validateList(
                new CreateUserListRequest("Başlık", tooLong, null, null)))
                .isInstanceOf(PartialUpdateValidationException.class);
    }
}
