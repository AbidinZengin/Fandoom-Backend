package com.example.fandoom_backend.community.service;

import com.example.fandoom_backend.common.config.JpaAuditingConfig;
import com.example.fandoom_backend.community.dto.ThreadPatchRequest;
import com.example.fandoom_backend.community.dto.ThreadRequest;
import com.example.fandoom_backend.community.entity.Portal;
import com.example.fandoom_backend.community.entity.PortalStatus;
import com.example.fandoom_backend.community.entity.Thread;
import com.example.fandoom_backend.community.entity.ThreadStatus;
import com.example.fandoom_backend.community.entity.ThreadSurface;
import com.example.fandoom_backend.community.mapper.PortalMapperImpl;
import com.example.fandoom_backend.community.mapper.ThreadMapperImpl;
import com.example.fandoom_backend.community.repository.PortalRepository;
import com.example.fandoom_backend.community.repository.ThreadRepository;
import com.example.fandoom_backend.movie.service.MovieService;
import com.example.fandoom_backend.series.service.SeriesService;
import com.example.fandoom_backend.user.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

// GERÇEK MySQL + GERÇEK eşzamanlılık (test transaction'ı YOK; her çağrı servisin kendi transaction'ında, tüm thread'ler
// aynı anda başlar). Üretim şemasını taklit etmek için fk_thread_portal ve fk_portal_membership_portal yoksa eklenir:
// FK'lar INSERT'te portal satırına S-kilit alır; kilit sırası yanlışsa eşzamanlı create/join deadlock'a düşer.
// Veri gerçekten commit edilir -> @AfterEach elle temizler.
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({JpaAuditingConfig.class, ThreadServiceImpl.class, PortalServiceImpl.class, PortalMembershipServiceImpl.class,
        PortalMapperImpl.class, ThreadMapperImpl.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ThreadPortalConcurrencyTest {

    private static final int N = 12;

    @Autowired private ThreadServiceImpl threadService;
    @Autowired private PortalMembershipServiceImpl membershipService;
    @Autowired private PortalRepository portalRepository;
    @Autowired private ThreadRepository threadRepository;
    @Autowired private JdbcTemplate jdbc;

    @MockitoBean private MovieService movieService;
    @MockitoBean private SeriesService seriesService;
    @MockitoBean private UserService userService;
    @MockitoBean private UserProfileService userProfileService;
    @MockitoBean private ThreadMediaService threadMediaService;

    private Portal portal;
    private Portal other;
    private final String token = "cc" + System.nanoTime();

    private void addFkIfMissing(String table, String constraint, String ddl) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS WHERE TABLE_SCHEMA = DATABASE() "
                + "AND TABLE_NAME = ? AND CONSTRAINT_NAME = ?", Integer.class, table, constraint);
        if (n != null && n == 0) {
            try {
                jdbc.execute(ddl);
            } catch (RuntimeException e) {
                // ör. geliştirme DB'sinde migration öncesi portal_id=0 satırları: FK'sız devam (test daha zayıf ama geçerli)
            }
        }
    }

    @BeforeEach
    void setUp() {
        addFkIfMissing("thread", "fk_thread_portal", "ALTER TABLE thread ADD CONSTRAINT fk_thread_portal FOREIGN KEY (portal_id) REFERENCES portal (id)");
        addFkIfMissing("portal_membership", "fk_portal_membership_portal",
                "ALTER TABLE portal_membership ADD CONSTRAINT fk_portal_membership_portal FOREIGN KEY (portal_id) REFERENCES portal (id) ON DELETE CASCADE");
        portal = portalRepository.saveAndFlush(Portal.builder().slug(token + "-a").nameTr("A").nameEn("A").build());
        other = portalRepository.saveAndFlush(Portal.builder().slug(token + "-b").nameTr("B").nameEn("B").build());
    }

    @AfterEach
    void cleanUp() {
        for (Portal p : List.of(portal, other)) {
            jdbc.update("DELETE FROM portal_membership WHERE portal_id = ?", p.getId());
            jdbc.update("DELETE FROM thread WHERE portal_id = ?", p.getId());
            jdbc.update("DELETE FROM portal WHERE id = ?", p.getId());
        }
    }

    private int publishedThreads(Portal p) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM thread WHERE portal_id = ? AND status = 'PUBLISHED'", Integer.class, p.getId());
    }

    private int threadCount(Portal p) {
        return jdbc.queryForObject("SELECT thread_count FROM portal WHERE id = ?", Integer.class, p.getId());
    }

    private int memberCount(Portal p) {
        return jdbc.queryForObject("SELECT member_count FROM portal WHERE id = ?", Integer.class, p.getId());
    }

    private <T> List<T> runAllAtOnce(List<Callable<T>> tasks) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(tasks.size());
        CountDownLatch ready = new CountDownLatch(tasks.size());
        CountDownLatch go = new CountDownLatch(1);
        List<Future<T>> futures = new ArrayList<>();
        for (Callable<T> task : tasks) {
            futures.add(pool.submit(() -> {
                ready.countDown();
                go.await();
                return task.call();
            }));
        }
        assertThat(ready.await(30, TimeUnit.SECONDS)).isTrue();
        go.countDown();
        List<T> results = new ArrayList<>();
        for (Future<T> f : futures) {
            results.add(f.get(90, TimeUnit.SECONDS));   // deadlock / istisna olursa burada patlar
        }
        pool.shutdown();
        return results;
    }

    private ThreadRequest request(int i) {
        return new ThreadRequest(ThreadSurface.DISCUSSION, "Eşzamanlı thread " + token + " " + i, "gövde", null, false,
                null, null, null, portal.getSlug());
    }

    private long createThread(Portal p, ThreadStatus status) {
        return threadRepository.saveAndFlush(Thread.builder().slug("cc-" + System.nanoTime()).surface(ThreadSurface.DISCUSSION)
                .title("concurrency seed").status(status).authorId(1L).portalId(p.getId()).build()).getId();
    }

    // ---- (2) create kilit sırası ----

    @Test
    void parallelCreatesAndJoinsOnTheSamePortal_noDeadlock_threadCountEqualsRealPublishedCount() throws Exception {
        List<Callable<Object>> tasks = new ArrayList<>();
        for (int i = 0; i < N; i++) {
            int n = i;
            tasks.add(() -> threadService.create(100L + n, false, request(n)));
            long user = 5000 + i;
            tasks.add(() -> membershipService.join(user, portal.getSlug()));
        }

        runAllAtOnce(tasks);

        assertThat(publishedThreads(portal)).isEqualTo(N);
        assertThat(threadCount(portal)).isEqualTo(N);          // sayaç == gerçek PUBLISHED sayısı
        assertThat(memberCount(portal)).isEqualTo(N);
    }

    // ---- (3) çift silme ----

    @Test
    void parallelDoubleDelete_decrementsCounterExactlyOnce() throws Exception {
        long keep1 = createThread(portal, ThreadStatus.PUBLISHED);
        long keep2 = createThread(portal, ThreadStatus.PUBLISHED);
        long victim = createThread(portal, ThreadStatus.PUBLISHED);
        jdbc.update("UPDATE portal SET thread_count = 3 WHERE id = ?", portal.getId());

        List<Callable<Object>> tasks = new ArrayList<>();
        for (int i = 0; i < N; i++) {
            tasks.add(() -> { threadService.delete(1L, false, victim); return null; });      // id variantı
        }
        // slug varyantı da aynı anda
        String victimSlug = jdbc.queryForObject("SELECT slug FROM thread WHERE id = ?", String.class, victim);
        for (int i = 0; i < 4; i++) {
            tasks.add(() -> { threadService.delete(1L, false, victimSlug); return null; });
        }

        runAllAtOnce(tasks);

        assertThat(threadCount(portal)).isEqualTo(2);           // 3 - 1: yalnız BİR çağrı düşürdü
        assertThat(publishedThreads(portal)).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT status FROM thread WHERE id = ?", String.class, victim)).isEqualTo("DELETED");
        assertThat(jdbc.queryForObject("SELECT status FROM thread WHERE id = ?", String.class, keep1)).isEqualTo("PUBLISHED");
        assertThat(jdbc.queryForObject("SELECT status FROM thread WHERE id = ?", String.class, keep2)).isEqualTo("PUBLISHED");
    }

    // ---- (3) taşıma kilidi: aynı thread'e eşzamanlı taşıma + silme ----

    @Test
    void parallelMoveAndDelete_ofTheSameThread_keepBothCountersConsistentWithRealRows() throws Exception {
        long t = createThread(portal, ThreadStatus.PUBLISHED);
        jdbc.update("UPDATE portal SET thread_count = 1 WHERE id = ?", portal.getId());

        List<Callable<Object>> tasks = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            tasks.add(() -> threadService.update(99L, true, t,
                    new ThreadPatchRequest(null, null, null, null, null, null, other.getSlug())));
            tasks.add(() -> { threadService.delete(1L, false, t); return null; });
        }

        runAllAtOnce(tasks);

        // Hangi sırayla olursa olsun sayaçlar gerçek PUBLISHED satırlarıyla birebir tutmalı ve negatif olmamalı.
        assertThat(threadCount(portal)).isEqualTo(publishedThreads(portal));
        assertThat(threadCount(other)).isEqualTo(publishedThreads(other));
        assertThat(threadCount(portal)).isGreaterThanOrEqualTo(0);
        assertThat(threadCount(other)).isGreaterThanOrEqualTo(0);
    }
}
