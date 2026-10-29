package com.example.fandoom_backend.notification.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

// Köprü: bildirim DB'ye COMMIT OLDUKTAN SONRA notification.created'a yollar. Geri alınan bildirim için çalışmaz.
@Component
@RequiredArgsConstructor
@Slf4j
class NotificationCreatedPublisher {

    private final KafkaTemplate<String, NotificationCreatedMessage> kafkaTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void on(NotificationCreatedEvent event) {
        NotificationCreatedMessage message = NotificationCreatedMessage.from(event);
        kafkaTemplate.send(NotificationKafkaTopics.NOTIFICATION_CREATED, message.partitionKey(), message)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        // Commit sonrası: hata bildirimi geri ALMAZ, olay kaybolur (outbox konusu).
                        log.error("Failed to publish notification-created event to Kafka: {}", message, ex);
                    } else {
                        log.info("[PUBLISH] notification {} -> recipient {} key={} partition={} offset={}",
                                message.notificationId(), message.recipientId(), message.partitionKey(),
                                result.getRecordMetadata().partition(), result.getRecordMetadata().offset());
                    }
                });
    }
}
