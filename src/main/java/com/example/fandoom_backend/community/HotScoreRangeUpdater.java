package com.example.fandoom_backend.community;

import com.example.fandoom_backend.community.entity.ThreadStatus;
import com.example.fandoom_backend.community.repository.ThreadRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

// hot_score hesabını tamamen veritabanında yapan toplu UPDATE'i, id aralığı başına KENDİ kısa
// transaction'ında çalıştırır. HotScoreJob'dan ayrı bir bean olmasının sebebi: @Transactional aynı sınıf
// içi çağrıda (self-invocation) proxy'yi atlar; job bütün çalışmayı tek transaction'a sarsaydı pencereleme
// anlamsızlaşırdı (tablo boyu kilit + uzun transaction).
@Component
@RequiredArgsConstructor
public class HotScoreRangeUpdater {

    private final ThreadRepository threadRepository;

    // PUBLISHED thread'lerin [minId, maxId] aralığı. Satır YÜKLENMEZ: PK üzerinde ilk/son eşleşen id
    // (ORDER BY id LIMIT 1) — tablo boyutundan bağımsız sabit maliyet.
    @Transactional(readOnly = true)
    public Optional<IdRange> publishedIdRange() {
        List<Long> first = threadRepository.findIdsByStatusAscending(ThreadStatus.PUBLISHED, PageRequest.of(0, 1));
        List<Long> last = threadRepository.findIdsByStatusDescending(ThreadStatus.PUBLISHED, PageRequest.of(0, 1));
        if (first.isEmpty() || last.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new IdRange(first.get(0), last.get(0)));
    }

    // [fromId, toId) aralığındaki PUBLISHED thread'lerin skorunu TEK SQL statement'ıyla günceller.
    // Dönen sayı, statement'ın eşleştirdiği satır sayısıdır (yalnızca değeri değişenler fiilen yazılır).
    @Transactional
    public int recalculate(long fromId, long toId, LocalDateTime now) {
        return threadRepository.recalculateHotScores(ThreadStatus.PUBLISHED.name(), fromId, toId, now);
    }

    public record IdRange(long minId, long maxId) {
    }
}
