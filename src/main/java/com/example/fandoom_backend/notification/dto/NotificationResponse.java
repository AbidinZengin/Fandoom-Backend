package com.example.fandoom_backend.notification.dto;

import com.example.fandoom_backend.account.entity.FollowState;
import com.example.fandoom_backend.notification.entity.NotificationType;

import java.time.LocalDateTime;

public record NotificationResponse(Long id, NotificationType type, boolean read, LocalDateTime createdAt,
                                   ActorSummary actor) {

    // followStatus: ALICININ actor ile ilişkisi (alıcı actor'ı takip ediyor mu) — FE "Geri takip et" butonunu bundan çizer.
    public record ActorSummary(String username, String avatarUrl, FollowState followStatus) {
    }
}
