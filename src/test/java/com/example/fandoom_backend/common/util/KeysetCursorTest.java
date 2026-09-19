package com.example.fandoom_backend.common.util;

import com.example.fandoom_backend.common.exception.InvalidReferenceException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KeysetCursorTest {

    @Test
    void encodeDecode_roundTrips_evenWhenValueContainsColonsAndSeparator() {
        KeysetCursor original = new KeysetCursor("2026-09-19T10:15:30.123456", 42L);

        KeysetCursor decoded = KeysetCursor.decode(original.encode());

        assertThat(decoded).isEqualTo(original);
    }

    @Test
    void decode_nullOrBlank_meansFirstPage() {
        assertThat(KeysetCursor.decode(null)).isNull();
        assertThat(KeysetCursor.decode("  ")).isNull();
    }

    @Test
    void decode_garbage_throwsInvalidReference() {
        assertThatThrownBy(() -> KeysetCursor.decode("%%%")).isInstanceOf(InvalidReferenceException.class);
        assertThatThrownBy(() -> KeysetCursor.decode("bm9zZXBhcmF0b3I")).isInstanceOf(InvalidReferenceException.class);
    }
}
