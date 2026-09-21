package com.example.fandoom_backend.community.repository;

import com.example.fandoom_backend.common.config.JpaAuditingConfig;
import com.example.fandoom_backend.community.entity.Portal;
import com.example.fandoom_backend.community.entity.PortalProduction;
import com.example.fandoom_backend.community.entity.PortalProductionType;
import com.example.fandoom_backend.community.entity.PortalStatus;
import com.example.fandoom_backend.community.entity.Thread;
import com.example.fandoom_backend.community.entity.ThreadStatus;
import com.example.fandoom_backend.community.entity.ThreadSurface;
import com.example.fandoom_backend.community.specification.ThreadSpecificationBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// BlogRepositoryTest/CommentRepositoryTest ile aynı desen: gerçek yerel MySQL, her test rollback edilir.
// JPQL (NOT EXISTS alt sorgusu, sayaç UPDATE'leri, bulk DELETE) ve Specification'lar yalnızca gerçek DB'de doğrulanabilir.
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaAuditingConfig.class)
class PortalRepositoryTest {

    @Autowired private TestEntityManager entityManager;
    @Autowired private PortalRepository portalRepository;
    @Autowired private PortalProductionRepository portalProductionRepository;
    @Autowired private ThreadRepository threadRepository;

    private Portal portal(PortalStatus status) {
        return entityManager.persist(Portal.builder()
                .slug("prt-" + System.nanoTime()).nameTr("Test").nameEn("Test").status(status).build());
    }

    private Thread thread(Portal portal, ThreadStatus status) {
        return entityManager.persist(Thread.builder()
                .slug("prt-thread-" + System.nanoTime()).surface(ThreadSurface.THEORY).title("portal repo test")
                .status(status).authorId(1L).portalId(portal.getId()).build());
    }

    @Test
    void findVisible_activeAndArchivedPortals_areVisible_hiddenPortalAndDeletedThreadAreNot() {
        Portal active = portal(PortalStatus.ACTIVE);
        Portal archived = portal(PortalStatus.ARCHIVED);
        Portal hidden = portal(PortalStatus.HIDDEN);
        Thread inActive = thread(active, ThreadStatus.PUBLISHED);
        Thread inArchived = thread(archived, ThreadStatus.PUBLISHED);
        Thread inHidden = thread(hidden, ThreadStatus.PUBLISHED);
        Thread deleted = thread(active, ThreadStatus.DELETED);
        Thread hiddenThread = thread(active, ThreadStatus.HIDDEN);
        entityManager.flush();
        entityManager.clear();

        assertThat(threadRepository.findVisibleById(inActive.getId())).isPresent();
        assertThat(threadRepository.findVisibleById(inArchived.getId())).isPresent();
        assertThat(threadRepository.findVisibleById(inHidden.getId())).isEmpty();
        assertThat(threadRepository.findVisibleById(deleted.getId())).isEmpty();
        assertThat(threadRepository.findVisibleById(hiddenThread.getId())).isEmpty();

        assertThat(threadRepository.findVisibleBySlug(inActive.getSlug())).isPresent();
        assertThat(threadRepository.findVisibleBySlug(inHidden.getSlug())).isEmpty();
        assertThat(threadRepository.findVisibleBySlug(deleted.getSlug())).isEmpty();
        assertThat(threadRepository.findVisibleById(-1L)).isEmpty();
    }

    @Test
    void isInHiddenPortal_trueOnlyForThreadsOfHiddenPortals() {
        Thread inHidden = thread(portal(PortalStatus.HIDDEN), ThreadStatus.PUBLISHED);
        Thread inActive = thread(portal(PortalStatus.ACTIVE), ThreadStatus.PUBLISHED);
        entityManager.flush();
        entityManager.clear();

        assertThat(threadRepository.isInHiddenPortal(inHidden.getId())).isTrue();
        assertThat(threadRepository.isInHiddenPortal(inActive.getId())).isFalse();
        assertThat(threadRepository.isInHiddenPortal(-1L)).isFalse();
    }

    @Test
    void specifications_filterByPortal_andExcludeHiddenPortals() {
        Portal a = portal(PortalStatus.ACTIVE);
        Portal b = portal(PortalStatus.ACTIVE);
        Portal hidden = portal(PortalStatus.HIDDEN);
        Thread ta = thread(a, ThreadStatus.PUBLISHED);
        Thread tb = thread(b, ThreadStatus.PUBLISHED);
        Thread th = thread(hidden, ThreadStatus.PUBLISHED);
        entityManager.flush();
        entityManager.clear();

        Specification<Thread> onlyA = Specification.allOf(
                ThreadSpecificationBuilder.isPublished(), ThreadSpecificationBuilder.hasPortalId(a.getId()));
        assertThat(threadRepository.findAll(onlyA)).extracting(Thread::getId).containsExactly(ta.getId());

        Specification<Thread> notHidden = Specification.allOf(
                ThreadSpecificationBuilder.isPublished(),
                ThreadSpecificationBuilder.hasIdIn(List.of(ta.getId(), tb.getId(), th.getId())),
                ThreadSpecificationBuilder.portalIdNotIn(Set.of(hidden.getId())));
        assertThat(threadRepository.findAll(notHidden)).extracting(Thread::getId)
                .containsExactlyInAnyOrder(ta.getId(), tb.getId());

        assertThat(ThreadSpecificationBuilder.hasPortalId(null)).isNull();
        assertThat(ThreadSpecificationBuilder.portalIdNotIn(Set.of())).isNull();
        assertThat(ThreadSpecificationBuilder.portalIdNotIn(null)).isNull();
    }

    @Test
    void threadCountCounters_areAtomicUpdates_andNeverGoBelowZero() {
        Portal p = portal(PortalStatus.ACTIVE);
        entityManager.flush();

        portalRepository.incrementThreadCount(p.getId());
        portalRepository.incrementThreadCount(p.getId());
        portalRepository.decrementThreadCount(p.getId());
        entityManager.clear();
        assertThat(portalRepository.findById(p.getId()).orElseThrow().getThreadCount()).isEqualTo(1);

        portalRepository.decrementThreadCount(p.getId());
        portalRepository.decrementThreadCount(p.getId()); // 0'ın altına inmez
        entityManager.clear();
        assertThat(portalRepository.findById(p.getId()).orElseThrow().getThreadCount()).isZero();
    }

    @Test
    void findIdsByStatus_returnsOnlyThatStatus() {
        Portal hidden = portal(PortalStatus.HIDDEN);
        Portal active = portal(PortalStatus.ACTIVE);
        entityManager.flush();

        List<Long> ids = portalRepository.findIdsByStatus(PortalStatus.HIDDEN);

        assertThat(ids).contains(hidden.getId()).doesNotContain(active.getId());
    }

    @Test
    void portalSlug_isUnique() {
        Portal p = portal(PortalStatus.ACTIVE);
        entityManager.flush();

        assertThatThrownBy(() -> {
            portalRepository.saveAndFlush(Portal.builder().slug(p.getSlug()).nameTr("x").nameEn("y").build());
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void productionSlug_canBelongToAtMostOnePortal() {
        Portal a = portal(PortalStatus.ACTIVE);
        Portal b = portal(PortalStatus.ACTIVE);
        String slug = "prt-prod-" + System.nanoTime();
        portalProductionRepository.saveAndFlush(PortalProduction.builder().portalId(a.getId())
                .productionType(PortalProductionType.SERIES).productionSlug(slug).build());

        assertThatThrownBy(() -> portalProductionRepository.saveAndFlush(PortalProduction.builder()
                .portalId(b.getId()).productionType(PortalProductionType.SERIES).productionSlug(slug).build()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void productionQueries_andBulkDelete() {
        Portal p = portal(PortalStatus.ACTIVE);
        String s1 = "prt-prod-a-" + System.nanoTime();
        String s2 = "prt-prod-b-" + System.nanoTime();
        portalProductionRepository.save(PortalProduction.builder().portalId(p.getId())
                .productionType(PortalProductionType.SERIES).productionSlug(s1).build());
        portalProductionRepository.save(PortalProduction.builder().portalId(p.getId())
                .productionType(PortalProductionType.MOVIE).productionSlug(s2).build());
        entityManager.flush();

        assertThat(portalProductionRepository.existsByPortalIdAndProductionSlug(p.getId(), s1)).isTrue();
        assertThat(portalProductionRepository.existsByPortalIdAndProductionSlug(p.getId(), "yok")).isFalse();
        assertThat(portalProductionRepository.findByPortalIdIn(List.of(p.getId()))).hasSize(2);

        portalProductionRepository.deleteByPortalIdAndProductionSlugIn(p.getId(), List.of(s1));

        assertThat(portalProductionRepository.findByPortalId(p.getId()))
                .extracting(PortalProduction::getProductionSlug).containsExactly(s2);
    }
}
