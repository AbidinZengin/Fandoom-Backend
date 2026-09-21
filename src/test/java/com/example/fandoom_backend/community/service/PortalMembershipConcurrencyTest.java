package com.example.fandoom_backend.community.service;

import com.example.fandoom_backend.common.config.JpaAuditingConfig;
import com.example.fandoom_backend.community.dto.PortalMembershipStatusResponse;
import com.example.fandoom_backend.community.entity.Portal;
import com.example.fandoom_backend.community.repository.PortalMembershipRepository;
import com.example.fandoom_backend.community.repository.PortalRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

// GERÇEK MySQL + GERÇEK eşzamanlılık: test transaction'ı YOK (NOT_SUPPORTED), her join/leave servisin kendi
// transaction'ında, 20 thread'le aynı anda. Veri gerçekten commit edilir -> @AfterEach ile elle temizlenir.
// Üretim şemasını taklit etmek için portal_membership.portal_id FK'sı (portal_migration.sql: fk_portal_membership_portal)
// yoksa eklenir: FK, INSERT'te portal satırına S-kilit alır; kilitsiz join'lerde S->X yükseltmesi deadlock üretirdi.
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({JpaAuditingConfig.class, PortalMembershipServiceImpl.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class PortalMembershipConcurrencyTest {

    private static final int THREADS = 20;

    @Autowired private PortalMembershipServiceImpl service;
    @Autowired private PortalRepository portalRepository;
    @Autowired private PortalMembershipRepository membershipRepository;
    @Autowired private JdbcTemplate jdbc;

    private Portal portal;

    @BeforeEach
    void setUp() {
        Integer fk = jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS WHERE TABLE_SCHEMA = DATABASE() "
                + "AND TABLE_NAME = 'portal_membership' AND CONSTRAINT_NAME = 'fk_portal_membership_portal'", Integer.class);
        if (fk != null && fk == 0) {
            jdbc.execute("ALTER TABLE portal_membership ADD CONSTRAINT fk_portal_membership_portal "
                    + "FOREIGN KEY (portal_id) REFERENCES portal (id) ON DELETE CASCADE");
        }
        portal = portalRepository.saveAndFlush(Portal.builder().slug("conc-" + System.nanoTime())
                .nameTr("Eşzamanlılık").nameEn("Concurrency").build());
    }

    @AfterEach
    void cleanUp() {
        jdbc.update("DELETE FROM portal_membership WHERE portal_id = ?", portal.getId());
        jdbc.update("DELETE FROM portal WHERE id = ?", portal.getId());
    }

    private int memberCount() {
        return jdbc.queryForObject("SELECT member_count FROM portal WHERE id = ?", Integer.class, portal.getId());
    }

    private int rows() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM portal_membership WHERE portal_id = ?", Integer.class, portal.getId());
    }

    // Tüm thread'ler kapıda bekler, aynı anda bırakılır.
    private List<PortalMembershipStatusResponse> runConcurrently(List<java.util.function.Supplier<PortalMembershipStatusResponse>> tasks)
            throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(tasks.size());
        CountDownLatch ready = new CountDownLatch(tasks.size());
        CountDownLatch go = new CountDownLatch(1);
        List<Future<PortalMembershipStatusResponse>> futures = new ArrayList<>();
        for (var task : tasks) {
            futures.add(pool.submit(() -> {
                ready.countDown();
                go.await();
                return task.get();
            }));
        }
        assertThat(ready.await(30, TimeUnit.SECONDS)).isTrue();
        go.countDown();
        List<PortalMembershipStatusResponse> results = new ArrayList<>();
        for (Future<PortalMembershipStatusResponse> f : futures) {
            results.add(f.get(60, TimeUnit.SECONDS)); // deadlock/istisna olursa burada patlar
        }
        pool.shutdown();
        return results;
    }

    @Test
    void twentyConcurrentJoinsByDifferentUsers_yieldExactlyTwentyMembers() throws Exception {
        List<java.util.function.Supplier<PortalMembershipStatusResponse>> tasks = new ArrayList<>();
        for (int i = 0; i < THREADS; i++) {
            long user = 1000 + i;
            tasks.add(() -> service.join(user, portal.getSlug()));
        }

        List<PortalMembershipStatusResponse> results = runConcurrently(tasks);

        assertThat(rows()).isEqualTo(THREADS);
        assertThat(memberCount()).isEqualTo(THREADS);
        assertThat(results).allMatch(PortalMembershipStatusResponse::member);
        // her yanıttaki sayı kilit altında sıralı okunduğundan 1..20 arasında ve hepsi FARKLI
        assertThat(results).extracting(PortalMembershipStatusResponse::memberCount)
                .containsExactlyInAnyOrder(java.util.stream.IntStream.rangeClosed(1, THREADS).boxed().toArray(Integer[]::new));
    }

    @Test
    void twentyConcurrentJoinsBySameUser_countOnce() throws Exception {
        List<java.util.function.Supplier<PortalMembershipStatusResponse>> tasks = new ArrayList<>();
        for (int i = 0; i < THREADS; i++) {
            tasks.add(() -> service.join(4242L, portal.getSlug()));
        }

        runConcurrently(tasks);

        assertThat(rows()).isEqualTo(1);
        assertThat(memberCount()).isEqualTo(1);
    }

    @Test
    void concurrentJoinsAndLeaves_keepCounterEqualToRows_neverNegative() throws Exception {
        for (long u = 1; u <= 10; u++) {
            service.join(u, portal.getSlug());
        }
        assertThat(memberCount()).isEqualTo(10);

        List<java.util.function.Supplier<PortalMembershipStatusResponse>> tasks = new ArrayList<>();
        for (long u = 1; u <= 10; u++) {
            long user = u;
            tasks.add(() -> service.leave(user, portal.getSlug()));      // 10 üye ayrılıyor
            tasks.add(() -> service.leave(user, portal.getSlug()));      // ... ikişer kez (ikincisi no-op)
        }
        for (long u = 100; u < 110; u++) {
            long user = u;
            tasks.add(() -> service.join(user, portal.getSlug()));       // 10 yeni üye
        }

        runConcurrently(tasks);

        assertThat(rows()).isEqualTo(10);                  // yalnız yeni 10 kalır
        assertThat(memberCount()).isEqualTo(rows());       // sayaç satırlarla birebir
        assertThat(memberCount()).isGreaterThanOrEqualTo(0);
    }

    @Test
    void sequentialJoinTwiceLeaveTwice_netEffectIsPlusOneThenMinusOne() {
        assertThat(service.join(9L, portal.getSlug()).memberCount()).isEqualTo(1);
        assertThat(service.join(9L, portal.getSlug()).memberCount()).isEqualTo(1);
        assertThat(memberCount()).isEqualTo(1);

        assertThat(service.leave(9L, portal.getSlug()).memberCount()).isZero();
        assertThat(service.leave(9L, portal.getSlug()).memberCount()).isZero();
        assertThat(memberCount()).isZero();
        assertThat(membershipRepository.existsByPortalIdAndUserId(portal.getId(), 9L)).isFalse();
    }
}
