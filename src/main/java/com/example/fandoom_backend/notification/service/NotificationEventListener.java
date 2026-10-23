package com.example.fandoom_backend.notification.service;

import com.example.fandoom_backend.account.event.ProfileFollowEvent;
import com.example.fandoom_backend.notification.entity.NotificationType;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

// Takip olaylarını bildirime çevirir. Senkron @EventListener: takip ile bildirim aynı transaction'da
// commit/rollback olur. Olay anlamları için bkz. ProfileFollowEvent.
@Component
@RequiredArgsConstructor
class NotificationEventListener {

    private final NotificationService notificationService;

    @EventListener
    void on(ProfileFollowEvent event) {
        switch (event.type()) {
            case FOLLOWED -> notificationService.record(event.actorId(), event.recipientId(), NotificationType.FOLLOW);
            case REQUESTED -> notificationService.record(event.actorId(), event.recipientId(),
                    NotificationType.FOLLOW_REQUEST);
            case ACCEPTED -> notificationService.record(event.actorId(), event.recipientId(),
                    NotificationType.FOLLOW_ACCEPTED);
            // Takip bırakıldı / istek geri çekildi: alıcıdaki okunmamış FOLLOW ve FOLLOW_REQUEST silinir.
            case WITHDRAWN -> notificationService.discardUnread(event.actorId(), event.recipientId(),
                    List.of(NotificationType.FOLLOW, NotificationType.FOLLOW_REQUEST));
            // Reddedildi: bildirim üretilmez; profil sahibindeki bayat istek bildirimi (actor=istek sahibi) silinir.
            case REQUEST_REJECTED -> notificationService.discardUnread(event.recipientId(), event.actorId(),
                    List.of(NotificationType.FOLLOW_REQUEST));
        }
    }
}
