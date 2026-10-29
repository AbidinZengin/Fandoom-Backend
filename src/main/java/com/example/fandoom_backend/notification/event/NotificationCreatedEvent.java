package com.example.fandoom_backend.notification.event;

import com.example.fandoom_backend.notification.entity.NotificationType;

// İç olay: bir bildirim DB'ye yazıldı. NotificationServiceImpl.record yayınlar, NotificationCreatedPublisher
// commit'ten sonra Kafka'ya (notification.created) çevirir. actorId null olabilir (ileride aktörsüz bildirimler).
public record NotificationCreatedEvent(Long notificationId, Long recipientId, Long actorId, NotificationType type) {
}
