package com.example.fandoom_backend.notification.controller;

import com.example.fandoom_backend.notification.service.NotificationStreamRegistry;
import com.example.fandoom_backend.user.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

// Anlık bildirim akışı (SSE). Tarayıcının native EventSource'u Authorization başlığı gönderemediği için istemci
// fetch tabanlı bir SSE kütüphanesi kullanır. /api/me/** SecurityConfig'te authenticated(); userId JWT'den çözülür.
@RestController
@RequestMapping("/api/me/notifications")
@RequiredArgsConstructor
public class NotificationStreamController {

    private final NotificationStreamRegistry streamRegistry;

    @GetMapping(path = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@AuthenticationPrincipal CustomUserDetails principal) {
        return streamRegistry.register(principal.getId());
    }
}
