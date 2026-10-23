package com.example.fandoom_backend.notification.entity;

// Genişlemeye açık: FE bilinmeyen tipi sessizce atlar, DB kolonu VARCHAR (yeni tip ALTER gerektirmez).
public enum NotificationType {
    FOLLOW, FOLLOW_REQUEST, FOLLOW_ACCEPTED
}
