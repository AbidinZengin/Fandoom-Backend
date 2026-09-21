package com.example.fandoom_backend.community;

import com.example.fandoom_backend.common.config.JpaAuditingConfig;
import com.example.fandoom_backend.community.entity.Portal;
import com.example.fandoom_backend.community.entity.Thread;
import com.example.fandoom_backend.community.entity.ThreadStatus;
import com.example.fandoom_backend.community.entity.ThreadSurface;
import com.example.fandoom_backend.community.repository.ThreadRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

// Gerçek MySQL: hot skorunun TEK kaynağı olan SQL formülünü ve pencere yürüyüşünü doğrular (BlogRepositoryTest ile
// aynı desen: her test rollback edilir, dev DB'de iz kalmaz). Formül Java'da artık YOK, bu yüzden değerler
// elle hesaplanmış beklenenlerle karşılaştırılır.
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({JpaAuditingConfig.class, HotScoreRangeUpdater.class})
class HotScoreJobIntegrationTest {

    @Autowired private TestEntityManager entityManager;
    @Autowired private ThreadRepository threadRepository;
    @Autowired private HotScoreRangeUpdater rangeUpdater;

    @Test
    void sqlFormula_matchesExpectedValues_forAgeLikesAndComments() {
        Thread aged = persist("hsq-aged", ThreadStatus.PUBLISHED, 10, 5);
        Thread fresh = persist("hsq-fresh", ThreadStatus.PUBLISHED, 1, 0);
        Thread idle = persist("hsq-idle", ThreadStatus.PUBLISHED, 0, 0);
        entityManager.flush();
        setCreatedAt(aged, LocalDateTime.now().minusHours(2).minusMinutes(1)); // tam 2 saat (küsurat atılır)
        setCreatedAt(idle, LocalDateTime.now().minusDays(30));
        entityManager.clear();

        new HotScoreJob(rangeUpdater, 10_000).recalculateHotScores();
        entityManager.clear();

        // (10 + 2*5) / (2 + 2)^1.5 = 20 / 8
        assertThat(hotScore(aged)).isCloseTo(2.5, within(1e-9));
        // yaş 0 saat: 1 / 2^1.5
        assertThat(hotScore(fresh)).isCloseTo(1 / Math.pow(2, 1.5), within(1e-9));
        assertThat(hotScore(idle)).isZero();
    }

    // Regresyon: created_at, job'ın "şimdi"sinden 2-3 saat İLERİDE ise TIMESTAMPDIFF -2 verir, payda POW(0, 1.5) = 0
    // olur ve MySQL "Division by 0" ile TÜM pencereyi (dolayısıyla job'ı) düşürürdü. Yaş 0'a kırpılmalı.
    @Test
    void futureDatedThread_doesNotBreakTheWindow_andIsTreatedAsAgeZero() {
        Thread future = persist("hsf-future", ThreadStatus.PUBLISHED, 4, 0);
        Thread normal = persist("hsf-normal", ThreadStatus.PUBLISHED, 4, 0);
        entityManager.flush();
        setCreatedAt(future, LocalDateTime.now().plusHours(2).plusMinutes(30)); // yaş -2 saat
        entityManager.clear();

        new HotScoreJob(rangeUpdater, 10_000).recalculateHotScores();
        entityManager.clear();

        double ageZeroScore = 4 / Math.pow(2, 1.5);
        assertThat(hotScore(future)).isCloseTo(ageZeroScore, within(1e-9));
        assertThat(hotScore(normal)).isCloseTo(ageZeroScore, within(1e-9)); // pencerenin geri kalanı da güncellendi
    }

    @Test
    void walksMultipleWindows_updatesOnlyPublished_andLeavesCountersAndUpdatedAtUntouched() {
        List<Thread> published = new ArrayList<>();
        for (int i = 1; i <= 7; i++) {
            published.add(persist("hsw-" + i, ThreadStatus.PUBLISHED, i, 0));
        }
        Thread deleted = persist("hsw-del", ThreadStatus.DELETED, 50, 0);
        entityManager.flush();
        entityManager.clear();
        LocalDateTime updatedAtBefore = entityManager.find(Thread.class, published.get(0).getId()).getUpdatedAt();
        entityManager.clear();

        // Pencere 3 id: 7 thread + 1 DELETED birden çok pencereye yayılır (ilk/son id, ORDER BY id LIMIT 1 ile bulunur).
        new HotScoreJob(rangeUpdater, 3).recalculateHotScores();
        entityManager.clear();

        for (int i = 0; i < 7; i++) {
            Thread reloaded = entityManager.find(Thread.class, published.get(i).getId());
            assertThat(reloaded.getHotScore()).isCloseTo((i + 1) / Math.pow(2, 1.5), within(1e-9));
            assertThat(reloaded.getLikeCount()).isEqualTo(i + 1);
        }
        assertThat(entityManager.find(Thread.class, published.get(0).getId()).getUpdatedAt()).isEqualTo(updatedAtBefore);
        assertThat(hotScore(deleted)).isZero();
    }

    @Test
    void publishedIdRange_returnsFirstAndLastPublishedId_ignoringDeleted() {
        Thread first = persist("hsr-1", ThreadStatus.PUBLISHED, 0, 0);
        persist("hsr-2", ThreadStatus.PUBLISHED, 0, 0);
        Thread last = persist("hsr-3", ThreadStatus.PUBLISHED, 0, 0);
        persist("hsr-del", ThreadStatus.DELETED, 0, 0); // en büyük id ama PUBLISHED değil
        entityManager.flush();

        var range = rangeUpdater.publishedIdRange().orElseThrow();

        assertThat(range.maxId()).isEqualTo(last.getId());
        assertThat(range.minId()).isLessThanOrEqualTo(first.getId());
        assertThat(threadRepository.findIdsByStatusDescending(ThreadStatus.PUBLISHED, PageRequest.of(0, 1)))
                .containsExactly(last.getId());
    }

    private double hotScore(Thread thread) {
        return entityManager.find(Thread.class, thread.getId()).getHotScore();
    }

    // created_at @CreatedDate ile persist anında ezilir (updatable=false); test için yaşı native UPDATE ile ayarlarız.
    private void setCreatedAt(Thread thread, LocalDateTime createdAt) {
        entityManager.getEntityManager()
                .createNativeQuery("UPDATE thread SET created_at = :ts WHERE id = :id")
                .setParameter("ts", createdAt)
                .setParameter("id", thread.getId())
                .executeUpdate();
    }

    // thread.portal_id NOT NULL (prod DB'de FK de var): her thread için gerçek bir portal (test sonunda rollback).
    private Long portalId() {
        return entityManager.persist(Portal.builder()
                .slug("hs-" + System.nanoTime()).nameTr("Test").nameEn("Test").build()).getId();
    }

    private Thread persist(String slug, ThreadStatus status, int likes, int comments) {
        Long portalId = portalId();
        return entityManager.persist(Thread.builder()
                .slug(slug + "-" + System.nanoTime())
                .surface(ThreadSurface.DISCUSSION)
                .title("hot score integration")
                .body("b")
                .status(status)
                .authorId(1L)
                .portalId(portalId)
                .likeCount(likes)
                .commentCount(comments)
                .build());
    }
}
