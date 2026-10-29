package com.example.fandoom_backend.account.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

// Köprü: ProfileFollowServiceImpl'in yayınladığı iç olayı, takip DB'ye COMMIT OLDUKTAN SONRA Kafka'ya yollar.
// Servis bu sınıfı bilmez. Geri alınan (rollback) takip için dinleyici hiç çalışmaz.
@Component
@RequiredArgsConstructor
@Slf4j
class ProfileFollowPublisher {

    private final KafkaTemplate<String, ProfileFollowMessage> kafkaTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void on(ProfileFollowEvent event) {
        ProfileFollowMessage message = ProfileFollowMessage.from(event);
        kafkaTemplate.send(AccountKafkaTopics.FOLLOW_EVENTS, message.partitionKey(), message)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        // Commit olduktan sonra olduğumuz için buradaki hata takibi geri ALMAZ; olay kaybolur (outbox konusu).
                        log.error("Failed to publish follow event to Kafka: {}", message, ex);
                    } else {
                        log.info("[PUBLISH] {} {}->{} key={} partition={} offset={}", message.type(),
                                message.actorId(), message.recipientId(), message.partitionKey(),
                                result.getRecordMetadata().partition(), result.getRecordMetadata().offset());
                    }
                });
    }
}
