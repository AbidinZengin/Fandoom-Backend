package com.example.fandoom_backend.notification.dto;

// SSE "notification" olayının gövdesi: yeni bildirim (liste satırıyla aynı şekil) + güncel okunmamış sayısı (rozet için).
public record NotificationStreamEvent(NotificationResponse notification, long unreadCount) {
}
