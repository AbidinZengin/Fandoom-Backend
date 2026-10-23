package com.example.fandoom_backend.notification.controller;

import com.example.fandoom_backend.common.dto.PageResponse;
import com.example.fandoom_backend.notification.dto.NotificationResponse;
import com.example.fandoom_backend.notification.dto.UnreadCountResponse;
import com.example.fandoom_backend.notification.service.NotificationService;
import com.example.fandoom_backend.user.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

// /api/me/** SecurityConfig'te authenticated(); userId her zaman JWT'den çözülür.
@RestController
@RequestMapping("/api/me/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public PageResponse<NotificationResponse> list(@AuthenticationPrincipal CustomUserDetails principal,
                                                    @RequestParam(defaultValue = "false") boolean unreadOnly,
                                                    @PageableDefault(size = 20) Pageable pageable) {
        return notificationService.list(principal.getId(), unreadOnly, pageable);
    }

    // Navbar rozeti için hafif: tek indeksli COUNT.
    @GetMapping("/unread-count")
    public UnreadCountResponse unreadCount(@AuthenticationPrincipal CustomUserDetails principal) {
        return new UnreadCountResponse(notificationService.unreadCount(principal.getId()));
    }

    @PostMapping("/{id}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markRead(@AuthenticationPrincipal CustomUserDetails principal, @PathVariable Long id) {
        notificationService.markRead(principal.getId(), id);
    }

    @PostMapping("/read-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markAllRead(@AuthenticationPrincipal CustomUserDetails principal) {
        notificationService.markAllRead(principal.getId());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal CustomUserDetails principal, @PathVariable Long id) {
        notificationService.delete(principal.getId(), id);
    }
}
