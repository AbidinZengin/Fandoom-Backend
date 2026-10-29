package com.example.fandoom_backend.notification.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

// Kullanıcı başına açık SSE bağlantıları (aynı kullanıcı birden çok sekme/cihaz açabilir). Yalnız bu uygulama kopyasının
// bağlantılarını tutar: birden çok kopya çalışırsa Kafka grubu mesajı başka kopyaya verebilir (bkz. NotificationCreatedListener).
@Component
@Slf4j
public class NotificationStreamRegistry {

    // Access token ömrüyle (15 dk) aynı: bağlantı eski bir token'la sonsuza dek açık kalmasın; istemci yeniden bağlanır.
    static final long TIMEOUT_MS = Duration.ofMinutes(15).toMillis();

    private final Map<Long, Set<SseEmitter>> emittersByUser = new ConcurrentHashMap<>();

    public SseEmitter register(Long userId) {
        SseEmitter emitter = new SseEmitter(TIMEOUT_MS);
        emittersByUser.computeIfAbsent(userId, id -> ConcurrentHashMap.newKeySet()).add(emitter);
        emitter.onCompletion(() -> remove(userId, emitter));
        emitter.onTimeout(() -> remove(userId, emitter));
        emitter.onError(e -> remove(userId, emitter));
        // İlk olay başlıkları hemen gönderir: istemcinin bağlantı açıldı bilgisi gecikmesin.
        try {
            emitter.send(SseEmitter.event().name("connected").data("ok"));
        } catch (IOException | IllegalStateException e) {
            drop(userId, emitter, e);
        }
        return emitter;
    }

    public boolean hasConnections(Long userId) {
        Set<SseEmitter> emitters = emittersByUser.get(userId);
        return emitters != null && !emitters.isEmpty();
    }

    public void send(Long userId, String eventName, Object data) {
        Set<SseEmitter> emitters = emittersByUser.get(userId);
        if (emitters == null) {
            return;
        }
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name(eventName).data(data, MediaType.APPLICATION_JSON));
            } catch (IOException | IllegalStateException e) {
                drop(userId, emitter, e);
            }
        }
    }

    // Ara sunucular/proxy'ler boşta kalan bağlantıyı kapatabilir; yorum satırı bağlantıyı canlı tutar ve ölü bağlantıyı da ayıklar.
    @Scheduled(fixedRate = 25_000)
    void heartbeat() {
        for (Map.Entry<Long, Set<SseEmitter>> entry : List.copyOf(emittersByUser.entrySet())) {
            for (SseEmitter emitter : entry.getValue()) {
                try {
                    emitter.send(SseEmitter.event().comment("keep-alive"));
                } catch (IOException | IllegalStateException e) {
                    drop(entry.getKey(), emitter, e);
                }
            }
        }
    }

    private void drop(Long userId, SseEmitter emitter, Exception cause) {
        log.debug("Dropping dead notification stream for user {}: {}", userId, cause.toString());
        remove(userId, emitter);
        emitter.completeWithError(cause);
    }

    private void remove(Long userId, SseEmitter emitter) {
        emittersByUser.computeIfPresent(userId, (id, emitters) -> {
            emitters.remove(emitter);
            return emitters.isEmpty() ? null : emitters;
        });
    }
}
