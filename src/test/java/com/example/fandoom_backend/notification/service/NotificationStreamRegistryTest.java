package com.example.fandoom_backend.notification.service;

import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class NotificationStreamRegistryTest {

    private static final Long USER = 7L;
    private static final Long OTHER = 42L;

    private final NotificationStreamRegistry registry = new NotificationStreamRegistry();

    @Test
    void register_marksUserAsConnectedOnlyForThatUser() {
        assertThat(registry.hasConnections(USER)).isFalse();

        SseEmitter emitter = registry.register(USER);

        assertThat(emitter.getTimeout()).isEqualTo(NotificationStreamRegistry.TIMEOUT_MS);
        assertThat(registry.hasConnections(USER)).isTrue();
        assertThat(registry.hasConnections(OTHER)).isFalse();
    }

    @Test
    void register_sameUserTwice_keepsBothConnections() {
        registry.register(USER);
        registry.register(USER);

        assertThat(registry.hasConnections(USER)).isTrue();
    }

    @Test
    void send_toUserWithoutConnection_isNoOp() {
        assertThatCode(() -> registry.send(OTHER, "notification", "payload")).doesNotThrowAnyException();
        assertThat(registry.hasConnections(OTHER)).isFalse();
    }

    @Test
    void send_toConnectedUser_doesNotThrow() {
        registry.register(USER);

        assertThatCode(() -> registry.send(USER, "notification", "payload")).doesNotThrowAnyException();
    }

    @Test
    void heartbeat_withNoConnections_doesNotThrow() {
        assertThatCode(registry::heartbeat).doesNotThrowAnyException();
    }
}
