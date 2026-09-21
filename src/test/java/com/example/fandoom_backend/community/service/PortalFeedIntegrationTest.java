package com.example.fandoom_backend.community.service;

import com.example.fandoom_backend.common.config.JpaAuditingConfig;
import com.example.fandoom_backend.common.dto.KeysetPageResponse;
import com.example.fandoom_backend.common.dto.PageResponse;
import com.example.fandoom_backend.common.exception.InvalidReferenceException;
import com.example.fandoom_backend.common.exception.ResourceNotFoundException;
import com.example.fandoom_backend.community.dto.ThreadPatchRequest;
import com.example.fandoom_backend.community.dto.ThreadSummaryResponse;
import com.example.fandoom_backend.community.entity.Portal;
import com.example.fandoom_backend.community.entity.PortalMembership;
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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// GERÇEK MySQL + gerçek ThreadServiceImpl/PortalServiceImpl/repository'ler (yalnız dış modül servisleri mock):
// kabul kriteri 5 (portal+surface+hot feed'i yalnız o portalı döner; scope=joined yalnız üye portalları döner) ve
// thread taşımanın sayaç etkisi. Her test rollback. Diğer verilerden yalıtım için tüm thread'ler benzersiz bir
// productionSlug (TOKEN) taşır ve sorgular bu filtreyle yapılır.
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({JpaAuditingConfig.class, ThreadServiceImpl.class, PortalServiceImpl.class, PortalMapperImpl.class,
        ThreadMapperImpl.class})
class PortalFeedIntegrationTest {

    @Autowired private TestEntityManager em;
    @Autowired private ThreadServiceImpl threadService;
    @Autowired private PortalRepository portalRepository;
    @Autowired private ThreadRepository threadRepository;

    @MockitoBean private MovieService movieService;
    @MockitoBean private SeriesService seriesService;
    @MockitoBean private UserService userService;
    @MockitoBean private UserProfileService userProfileService;
    @MockitoBean private ThreadMediaService threadMediaService;

    private final String token = "tok-" + System.nanoTime();
    private static final PageRequest PAGE = PageRequest.of(0, 50);

    private Portal portal(String suffix, PortalStatus status) {
        return em.persist(Portal.builder().slug(token + "-" + suffix).nameTr("T " + suffix).nameEn("E " + suffix)
                .status(status).build());
    }

    private Thread thread(Portal portal, ThreadSurface surface, double hot, int likes, ThreadStatus status) {
        return em.persist(Thread.builder().slug("t-" + System.nanoTime()).surface(surface).title("portal feed test")
                .status(status).authorId(1L).portalId(portal.getId()).productionSlug(token).hotScore(hot)
                .likeCount(likes).build());
    }

    private Thread thread(Portal portal, ThreadSurface surface, double hot) {
        return thread(portal, surface, hot, 0, ThreadStatus.PUBLISHED);
    }

    private void member(Portal p, long userId) {
        em.persist(PortalMembership.builder().portalId(p.getId()).userId(userId).joinedAt(LocalDateTime.now()).build());
    }

    private static List<Long> ids(PageResponse<ThreadSummaryResponse> page) {
        return page.content().stream().map(ThreadSummaryResponse::id).toList();
    }

    // ---- kabul kriteri 5: /feed?portal=X&surface=THEORY&sort=hot ----

    @Test
    void feedByPortalSurfaceAndHotSort_returnsOnlyThatPortalsThreadsInHotOrder() {
        Portal westeros = portal("westeros", PortalStatus.ACTIVE);
        Portal other = portal("other", PortalStatus.ACTIVE);
        Thread hot5 = thread(westeros, ThreadSurface.THEORY, 5.0);
        Thread hot9 = thread(westeros, ThreadSurface.THEORY, 9.0);
        thread(westeros, ThreadSurface.DISCUSSION, 100.0);     // farklı surface
        thread(other, ThreadSurface.THEORY, 50.0);             // farklı portal
        em.flush();
        em.clear();

        PageResponse<ThreadSummaryResponse> page = threadService.list(
                ThreadSurface.THEORY, westeros.getSlug(), token, null, "hot", null, PAGE);

        assertThat(ids(page)).containsExactly(hot9.getId(), hot5.getId());
        assertThat(page.content()).allSatisfy(t -> {
            assertThat(t.portal().slug()).isEqualTo(westeros.getSlug());
            assertThat(t.portal().name()).isNotBlank();
        });
    }

    @Test
    void feedWithoutPortal_neverListsHiddenPortalThreads_andHiddenPortalFeedIs404() {
        Portal active = portal("active", PortalStatus.ACTIVE);
        Portal archived = portal("archived", PortalStatus.ARCHIVED);
        Portal hidden = portal("hidden", PortalStatus.HIDDEN);
        Thread a = thread(active, ThreadSurface.THEORY, 1.0);
        Thread r = thread(archived, ThreadSurface.THEORY, 2.0);
        Thread h = thread(hidden, ThreadSurface.THEORY, 3.0);
        em.flush();
        em.clear();

        assertThat(ids(threadService.list(null, null, token, null, "hot", null, PAGE)))
                .containsExactlyInAnyOrder(a.getId(), r.getId()).doesNotContain(h.getId());
        assertThat(ids(threadService.list(null, archived.getSlug(), token, null, "hot", null, PAGE)))
                .containsExactly(r.getId());                                         // ARCHIVED okunabilir
        assertThatThrownBy(() -> threadService.list(null, hidden.getSlug(), token, null, "hot", null, PAGE))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> threadService.list(null, "yok-" + token, token, null, "hot", null, PAGE))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ---- kabul kriteri 5: scope=joined ----

    @Test
    void joinedScope_returnsOnlyMemberPortalsThreads_excludingHiddenEvenIfMember() {
        Portal a = portal("a", PortalStatus.ACTIVE);
        Portal archived = portal("arch", PortalStatus.ARCHIVED);
        Portal b = portal("b", PortalStatus.ACTIVE);
        Portal hidden = portal("hid", PortalStatus.HIDDEN);
        Thread ta = thread(a, ThreadSurface.THEORY, 4.0);
        Thread tr = thread(archived, ThreadSurface.DISCUSSION, 3.0);
        thread(b, ThreadSurface.THEORY, 9.0);                    // üye değil
        thread(hidden, ThreadSurface.THEORY, 8.0);               // üye ama HIDDEN
        member(a, 77L);
        member(archived, 77L);
        member(hidden, 77L);
        em.flush();
        em.clear();

        assertThat(ids(threadService.listJoined(77L, null, null, token, null, "hot", PAGE)))
                .containsExactly(ta.getId(), tr.getId());        // hot: 4.0 > 3.0
        assertThat(ids(threadService.listJoined(77L, ThreadSurface.THEORY, null, token, null, "hot", PAGE)))
                .containsExactly(ta.getId());                    // surface ile birleşir
        assertThat(ids(threadService.listJoined(77L, null, a.getSlug(), token, null, "hot", PAGE)))
                .containsExactly(ta.getId());                    // portal ile birleşir (üye olunan portal)
        assertThat(ids(threadService.listJoined(77L, null, b.getSlug(), token, null, "hot", PAGE)))
                .isEmpty();                                      // üye olunmayan portal -> boş
        assertThat(ids(threadService.listJoined(77L, null, archived.getSlug(), token, null, "new", PAGE)))
                .containsExactly(tr.getId());
        assertThatThrownBy(() -> threadService.listJoined(77L, null, hidden.getSlug(), token, null, "hot", PAGE))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> threadService.listJoined(77L, null, "yok-" + token, token, null, "hot", PAGE))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void joinedScope_userWithoutMemberships_getsEmptyPage_andCursorVariantWorks() {
        Portal a = portal("a", PortalStatus.ACTIVE);
        Thread t1 = thread(a, ThreadSurface.THEORY, 3.0);
        Thread t2 = thread(a, ThreadSurface.THEORY, 2.0);
        Thread t3 = thread(a, ThreadSurface.THEORY, 1.0);
        member(a, 5L);
        em.flush();
        em.clear();

        assertThat(threadService.listJoined(999_999_001L, null, null, token, null, "hot", PAGE).content()).isEmpty();
        assertThat(threadService.listJoinedByCursor(999_999_001L, null, null, token, null, "hot", null, 20).content()).isEmpty();

        KeysetPageResponse<ThreadSummaryResponse> first = threadService.listJoinedByCursor(5L, null, null, token, null, "hot", null, 2);
        assertThat(first.content()).extracting(ThreadSummaryResponse::id).containsExactly(t1.getId(), t2.getId());
        assertThat(first.hasNext()).isTrue();
        KeysetPageResponse<ThreadSummaryResponse> second =
                threadService.listJoinedByCursor(5L, null, null, token, null, "hot", first.nextCursor(), 2);
        assertThat(second.content()).extracting(ThreadSummaryResponse::id).containsExactly(t3.getId());
        assertThat(second.hasNext()).isFalse();
    }

    // ---- thread taşıma ----

    private ThreadPatchRequest move(String portalSlug) {
        return new ThreadPatchRequest(null, null, null, null, null, null, portalSlug);
    }

    private int threadCount(Portal p) {
        em.flush();
        em.clear();
        return portalRepository.findById(p.getId()).orElseThrow().getThreadCount();
    }

    @Test
    void moderatorMove_movesPublishedThreadAndCountersAtomically_ownerIs403() {
        Portal from = em.persist(Portal.builder().slug(token + "-from").nameTr("F").nameEn("F").threadCount(1).build());
        Portal to = portal("to", PortalStatus.ACTIVE);
        Thread t = thread(from, ThreadSurface.THEORY, 1.0);
        em.flush();

        assertThatThrownBy(() -> threadService.update(1L, false, t.getId(), move(to.getSlug())))   // sahip (moderatör değil)
                .isInstanceOf(AccessDeniedException.class);
        assertThat(threadCount(from)).isEqualTo(1);
        assertThat(threadCount(to)).isZero();

        var response = threadService.update(99L, true, t.getId(), move(to.getSlug()));             // moderatör
        assertThat(response.portal().slug()).isEqualTo(to.getSlug());
        assertThat(threadRepository.findById(t.getId()).orElseThrow().getPortalId()).isEqualTo(to.getId());
        assertThat(threadCount(from)).isZero();
        assertThat(threadCount(to)).isEqualTo(1);
    }

    @Test
    void move_deletedThreadDoesNotTouchCounters_sameSlugIsNoop() {
        Portal from = em.persist(Portal.builder().slug(token + "-f2").nameTr("F").nameEn("F").threadCount(0).build());
        Portal to = portal("t2", PortalStatus.ACTIVE);
        Thread deleted = thread(from, ThreadSurface.THEORY, 1.0, 0, ThreadStatus.DELETED);
        Thread live = thread(from, ThreadSurface.THEORY, 1.0);
        em.flush();

        threadService.update(99L, true, deleted.getId(), move(to.getSlug()));
        assertThat(threadCount(from)).isZero();       // DELETED thread sayaçta değildi, düşmez
        assertThat(threadCount(to)).isZero();         // ve hedefte de artmaz

        threadService.update(99L, true, live.getId(), move(from.getSlug()));   // aynı portal = no-op
        assertThat(threadCount(from)).isZero();
    }

    @Test
    void move_targetArchivedIs400_hiddenOrMissingIs404_ownerSameSlugIsIgnored() {
        Portal from = portal("f3", PortalStatus.ACTIVE);
        Portal archived = portal("arc3", PortalStatus.ARCHIVED);
        Portal hidden = portal("hid3", PortalStatus.HIDDEN);
        Thread t = thread(from, ThreadSurface.THEORY, 1.0);
        em.flush();

        assertThatThrownBy(() -> threadService.update(99L, true, t.getId(), move(archived.getSlug())))
                .isInstanceOf(InvalidReferenceException.class);
        assertThatThrownBy(() -> threadService.update(99L, true, t.getId(), move(hidden.getSlug())))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> threadService.update(99L, true, t.getId(), move("yok-" + token)))
                .isInstanceOf(ResourceNotFoundException.class);

        // sahip, mevcut portalın slug'ını gönderirse (FE formu her zaman gönderebilir) taşıma isteği yok sayılır, 403 değil
        threadService.update(1L, false, t.getId(), move(from.getSlug()));
        assertThat(threadRepository.findById(t.getId()).orElseThrow().getPortalId()).isEqualTo(from.getId());
    }
}
