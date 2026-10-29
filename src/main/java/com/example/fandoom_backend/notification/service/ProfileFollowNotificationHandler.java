package com.example.fandoom_backend.notification.service;

import com.example.fandoom_backend.account.event.ProfileFollowMessage;
import com.example.fandoom_backend.notification.entity.NotificationType;
import com.example.fandoom_backend.notification.entity.ProcessedEvent;
import com.example.fandoom_backend.notification.repository.ProcessedEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// Takip mesajlarını bildirime çevirir (Kafka dinleyicisi çağırır). Olay anlamları için bkz. ProfileFollowEvent.
// Tekrar eleme ve bildirim AYNI transaction'da: ya ikisi de yazılır ya da hiçbiri.
@Service
@RequiredArgsConstructor
@Slf4j
public class ProfileFollowNotificationHandler {

    private final ProcessedEventRepository processedEventRepository;
    private final NotificationService notificationService;

    @Transactional
    public void handle(String consumerGroup, ProfileFollowMessage message) {
        if (processedEventRepository.existsByConsumerGroupAndEventId(consumerGroup, message.eventId())) {
            log.info("Duplicate event skipped: eventId={}", message.eventId());
            return;
        }
        // Önce "işlendi" işaretlenir: aynı anda gelen kopya UNIQUE ihlaliyle burada düşer, bildirim yazılmadan.
        processedEventRepository.saveAndFlush(new ProcessedEvent(consumerGroup, message.eventId()));

        Long actor = message.actorId();
        Long recipient = message.recipientId();
        switch (message.type()) {
            case FOLLOWED -> notificationService.record(actor, recipient, NotificationType.FOLLOW);
            case REQUESTED -> notificationService.record(actor, recipient, NotificationType.FOLLOW_REQUEST);
            case ACCEPTED -> notificationService.record(actor, recipient, NotificationType.FOLLOW_ACCEPTED);
            // Takip bırakıldı / istek geri çekildi: alıcıdaki okunmamış FOLLOW ve FOLLOW_REQUEST silinir.
            case WITHDRAWN -> notificationService.discardUnread(actor, recipient,
                    List.of(NotificationType.FOLLOW, NotificationType.FOLLOW_REQUEST));
            // Reddedildi: bildirim üretilmez; profil sahibindeki bayat istek bildirimi (actor=istek sahibi) silinir.
            case REQUEST_REJECTED -> notificationService.discardUnread(recipient, actor,
                    List.of(NotificationType.FOLLOW_REQUEST));
        }
    }
}
