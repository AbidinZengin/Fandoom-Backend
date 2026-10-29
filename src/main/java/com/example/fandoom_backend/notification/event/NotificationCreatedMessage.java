package com.example.fandoom_backend.notification.event;

import com.example.fandoom_backend.notification.entity.NotificationType;

import java.time.Instant;
import java.util.UUID;

// Kafka'ya giden "bildirim oluştu" mesajı (notification.created); teslimat kanalları (anlık, e-posta...) dinler.
// Hazır metin TAŞIMAZ: metin dile/kanala göre teslimat tarafında üretilir. version: şema değişirse eski dinleyiciler
// ayırt edebilsin. DB'ye yazılmaz.
public record NotificationCreatedMessage(String eventId, int version, Long notificationId, Long recipientId,
                                         Long actorId, NotificationType type, Instant occurredAt) {

    public static final int CURRENT_VERSION = 1;

    public static NotificationCreatedMessage from(NotificationCreatedEvent event) {
        return new NotificationCreatedMessage(UUID.randomUUID().toString(), CURRENT_VERSION,
                event.notificationId(), event.recipientId(), event.actorId(), event.type(), Instant.now());
    }

    // Aynı alıcının bildirimleri aynı partition'a düşsün: sıra korunur, farklı alıcılar paralel işlenir.
    public String partitionKey() {
        return String.valueOf(recipientId);
    }
}
