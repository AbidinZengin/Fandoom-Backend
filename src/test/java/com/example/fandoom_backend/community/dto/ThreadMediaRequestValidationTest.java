package com.example.fandoom_backend.community.dto;

import com.example.fandoom_backend.community.entity.ThreadMediaType;
import com.example.fandoom_backend.community.entity.ThreadSurface;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

// Controller'daki @Valid'in gördüğü kısıtlar: 7 medya -> 400 (MethodArgumentNotValidException) buradan gelir.
class ThreadMediaRequestValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private static List<ThreadMediaRequest> media(int count) {
        return IntStream.range(0, count)
                .mapToObj(i -> new ThreadMediaRequest(ThreadMediaType.IMAGE, "https://res.cloudinary.com/c/image/upload/" + i + ".webp"))
                .toList();
    }

    private ThreadRequest request(List<ThreadMediaRequest> media) {
        return new ThreadRequest(ThreadSurface.DISCUSSION, "Yeterince uzun bir başlık", "gövde", null, false, null, null, media);
    }

    @Test
    void sixMedia_isValid_sevenIsNot() {
        assertThat(validator.validate(request(media(6)))).isEmpty();
        assertThat(validator.validate(request(media(7)))).extracting(v -> v.getPropertyPath().toString())
                .containsExactly("media");
    }

    @Test
    void nullMedia_isValid() {
        assertThat(validator.validate(request(null))).isEmpty();
    }

    @Test
    void mediaItem_requiresTypeAndUrl() {
        var violations = validator.validate(request(List.of(new ThreadMediaRequest(null, " "))));

        assertThat(violations).extracting(v -> v.getPropertyPath().toString())
                .anyMatch(p -> p.contains("type")).anyMatch(p -> p.contains("url"));
    }

    @Test
    void mediaUrl_over500Chars_isInvalid() {
        var violations = validator.validate(request(List.of(
                new ThreadMediaRequest(ThreadMediaType.IMAGE, "https://res.cloudinary.com/" + "a".repeat(500)))));

        assertThat(violations).isNotEmpty();
    }

    @Test
    void patchRequest_sevenMedia_isInvalid() {
        var patch = new ThreadPatchRequest(null, null, null, null, null, media(7));

        assertThat(validator.validate(patch)).isNotEmpty();
    }
}
