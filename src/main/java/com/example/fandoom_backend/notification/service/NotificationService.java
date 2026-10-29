package com.example.fandoom_backend.notification.service;

import com.example.fandoom_backend.common.dto.PageResponse;
import com.example.fandoom_backend.notification.dto.NotificationResponse;
import com.example.fandoom_backend.notification.entity.NotificationType;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.Optional;

public interface NotificationService {

    // ---- üretim (olay dinleyicisi kullanır) ----

    // Kendine bildirim üretilmez; aynı (actor, recipient, type) için son 24 saatte bildirim varsa yenisi üretilmez.
    void record(Long actorId, Long recipientId, NotificationType type);
    // actor -> recipient yönündeki OKUNMAMIŞ bildirimleri siler (okunmuşlara dokunulmaz).
    void discardUnread(Long actorId, Long recipientId, Collection<NotificationType> types);

    // ---- alıcı tarafı (recipientId JWT'den) ----

    PageResponse<NotificationResponse> list(Long recipientId, boolean unreadOnly, Pageable pageable);
    // Tek bildirim, liste satırıyla aynı zenginleştirmeyle (anlık teslimat için). Başkasının/olmayan/silinmiş aktörlü → boş.
    Optional<NotificationResponse> get(Long recipientId, Long id);
    long unreadCount(Long recipientId);
    // Idempotent. Başkasının veya olmayan id → 404.
    void markRead(Long recipientId, Long id);
    void markAllRead(Long recipientId);
    // Başkasının veya olmayan id → 404.
    void delete(Long recipientId, Long id);
}
