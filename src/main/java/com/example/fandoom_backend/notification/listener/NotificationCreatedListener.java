package com.example.fandoom_backend.notification.listener;

import com.example.fandoom_backend.notification.dto.NotificationStreamEvent;
import com.example.fandoom_backend.notification.event.NotificationCreatedMessage;
import com.example.fandoom_backend.notification.event.NotificationKafkaTopics;
import com.example.fandoom_backend.notification.service.NotificationService;
import com.example.fandoom_backend.notification.service.NotificationStreamRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

// notification.created'ı dinler ve alıcının açık SSE bağlantılarına anlık iletir (kanal: uygulama içi).
// Ayrı grup (notification-delivery): bildirimi üreten grupla (notification-service) bağımsız ilerler.
// default.type: tip header'ı olmayan (elle yazılmış) mesajlar da doğru sınıfa çevrilsin.
// Sınır: yalnız bu kopyanın bağlantılarına yazar; birden çok kopya çalışırsa Redis pub/sub gibi bir yayın gerekir.
@Component
@RequiredArgsConstructor
@Slf4j
class NotificationCreatedListener {

    static final String EVENT_NAME = "notification";

    private final NotificationStreamRegistry streamRegistry;
    private final NotificationService notificationService;

    @KafkaListener(topics = NotificationKafkaTopics.NOTIFICATION_CREATED, groupId = "notification-delivery",
            properties = "spring.json.value.default.type=com.example.fandoom_backend.notification.event.NotificationCreatedMessage")
    void on(ConsumerRecord<String, NotificationCreatedMessage> record) {
        NotificationCreatedMessage message = record.value();
        log.info("[LISTEN] notification {} {} actor={} -> recipient {} eventId={} partition={} offset={}",
                message.notificationId(), message.type(), message.actorId(), message.recipientId(),
                message.eventId(), record.partition(), record.offset());

        Long recipientId = message.recipientId();
        // Çevrimdışı kullanıcı: bildirim zaten DB'de, uygulamayı açınca listeden çeker; zenginleştirme sorgularına gerek yok.
        if (!streamRegistry.hasConnections(recipientId)) {
            return;
        }
        // Anlık iletim en iyi çaba: hata mesajı yeniden denetmesin (aynı bildirim iki kez itilmesin), yalnız loglanır.
        try {
            notificationService.get(recipientId, message.notificationId()).ifPresent(notification ->
                    streamRegistry.send(recipientId, EVENT_NAME,
                            new NotificationStreamEvent(notification, notificationService.unreadCount(recipientId))));
        } catch (RuntimeException e) {
            log.error("Failed to push notification {} to recipient {}", message.notificationId(), recipientId, e);
        }
    }
}
