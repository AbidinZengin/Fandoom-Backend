package com.example.fandoom_backend.community;

import com.example.fandoom_backend.community.entity.Thread;
import com.example.fandoom_backend.community.entity.ThreadStatus;
import com.example.fandoom_backend.community.repository.ThreadRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Faz 1 "hot" sıralaması için basit bir yeniden hesaplama job'ı — Reddit-tarzı
 * zaman-azalışlı skor (like/comment ağırlıklı, yaş arttıkça payda büyüyerek
 * skoru düşürür). Faz 1'de sayfalama yok: her çalıştırmada tüm PUBLISHED
 * thread'ler bellekte yeniden hesaplanır (RevokedTokenCleanupScheduler ile
 * aynı desen — dirty-checking'e bırakılır, bulk update kullanılmaz).
 */
@Component
@RequiredArgsConstructor
public class HotScoreJob {

    private static final Logger log = LoggerFactory.getLogger(HotScoreJob.class);

    private final ThreadRepository threadRepository;

    @Scheduled(fixedRate = 900000)
    @Transactional
    public void recalculateHotScores() {
        List<Thread> threads = threadRepository.findByStatus(ThreadStatus.PUBLISHED);
        LocalDateTime now = LocalDateTime.now();
        for (Thread thread : threads) {
            long ageHours = Duration.between(thread.getCreatedAt(), now).toHours();
            double hotScore = (thread.getLikeCount() + 2.0 * thread.getCommentCount())
                    / Math.pow(ageHours + 2, 1.5);
            thread.setHotScore(hotScore);
        }
        log.info("HotScoreJob: {} thread için hotScore yeniden hesaplandı", threads.size());
    }
}
