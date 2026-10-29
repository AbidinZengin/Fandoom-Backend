package com.example.fandoom_backend.notification.listener;

import com.example.fandoom_backend.account.event.AccountKafkaTopics;
import com.example.fandoom_backend.account.event.ProfileFollowMessage;
import com.example.fandoom_backend.notification.service.ProfileFollowNotificationHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

// account.follow-events'i dinler. Bağımlılık tek yönlü: notification -> account (account notification'ı bilmez).
// İnce tutuldu: iş mantığı ve transaction ProfileFollowNotificationHandler'da.
@Component
@RequiredArgsConstructor
@Slf4j
class ProfileFollowListener {

    private static final String GROUP = "notification-service";

    private final ProfileFollowNotificationHandler handler;

    @KafkaListener(topics = AccountKafkaTopics.FOLLOW_EVENTS, groupId = GROUP)
    void on(ConsumerRecord<String, ProfileFollowMessage> record) {
        ProfileFollowMessage message = record.value();
        log.info("[LISTEN] {} {}->{} eventId={} partition={} offset={}", message.type(), message.actorId(),
                message.recipientId(), message.eventId(), record.partition(), record.offset());
        try {
            handler.handle(GROUP, message);
        } catch (DataIntegrityViolationException e) {
            // Aynı eventId'nin eşzamanlı kopyası: UNIQUE ihlali, transaction geri alındı; olay zaten işleniyor/işlendi.
            log.info("Concurrent duplicate skipped: eventId={}", message.eventId());
        }
    }
}
