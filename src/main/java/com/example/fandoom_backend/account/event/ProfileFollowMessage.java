package com.example.fandoom_backend.account.event;

import java.time.Instant;
import java.util.UUID;

// Kafka'ya giden takip olayı (account.follow-events). İç olay ProfileFollowEvent'ten ayrı bir sözleşme:
// eventId consumer'da tekrar eleme (idempotency), occurredAt hata ayıklama içindir. DB'ye yazılmaz.
// actorId/recipientId anlamları ProfileFollowEvent ile aynıdır.
public record ProfileFollowMessage(String eventId, Type type, Long actorId, Long recipientId, Instant occurredAt) {

    public enum Type {
        FOLLOWED, REQUESTED, ACCEPTED, WITHDRAWN, REQUEST_REJECTED
    }

    public static ProfileFollowMessage from(ProfileFollowEvent event) {
        return new ProfileFollowMessage(UUID.randomUUID().toString(), Type.valueOf(event.type().name()),
                event.actorId(), event.recipientId(), Instant.now());
    }

    // Aynı iki kişinin olayları (takip, bırakma, istek, kabul...) YÖNDEN BAĞIMSIZ aynı partition'a düşsün ki
    // sıra bozulmasın. Küçük id önce yazılır: (3,7) ve (7,3) aynı anahtarı verir.
    public String partitionKey() {
        return Math.min(actorId, recipientId) + "-" + Math.max(actorId, recipientId);
    }
}
