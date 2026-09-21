package com.example.fandoom_backend.community.service;

import com.example.fandoom_backend.common.config.JpaAuditingConfig;
import com.example.fandoom_backend.common.dto.PageResponse;
import com.example.fandoom_backend.common.exception.InvalidReferenceException;
import com.example.fandoom_backend.common.exception.ResourceNotFoundException;
import com.example.fandoom_backend.community.dto.PortalDetailResponse;
import com.example.fandoom_backend.community.dto.PortalSummaryResponse;
import com.example.fandoom_backend.community.entity.Comment;
import com.example.fandoom_backend.community.entity.CommentStatus;
import com.example.fandoom_backend.community.entity.CommentSubjectType;
import com.example.fandoom_backend.community.entity.Portal;
import com.example.fandoom_backend.community.entity.PortalMembership;
import com.example.fandoom_backend.community.entity.PortalProduction;
import com.example.fandoom_backend.community.entity.PortalProductionType;
import com.example.fandoom_backend.community.entity.PortalStatus;
import com.example.fandoom_backend.community.entity.Thread;
import com.example.fandoom_backend.community.entity.ThreadStatus;
import com.example.fandoom_backend.community.entity.ThreadSurface;
import com.example.fandoom_backend.community.mapper.PortalMapperImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Gerçek MySQL (BlogRepositoryTest deseni, her test rollback): trending JPQL'i, q (LIKE escape), sıralamalar, isMember/
// productionSlugs toplu çözümü. Diğer (seed) portallar sonuçları bozmasın diye her test benzersiz bir ad belirteci
// (TOKEN) kullanır ve q=TOKEN ile yalnız kendi portallarını listeler.
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({JpaAuditingConfig.class, PortalQueryServiceImpl.class, PortalMapperImpl.class})
class PortalQueryServiceDataJpaTest {

    @Autowired private TestEntityManager em;
    @Autowired private PortalQueryServiceImpl service;

    private final String token = "zq" + System.nanoTime();

    @AfterEach
    void resetLocale() {
        LocaleContextHolder.resetLocaleContext();
    }

    private Portal portal(String suffix, PortalStatus status, int sortOrder, int members) {
        return em.persist(Portal.builder().slug(token + "-" + suffix).nameTr(token + " " + suffix + " tr")
                .nameEn(token + " " + suffix + " en").status(status).sortOrder(sortOrder).memberCount(members).build());
    }

    private Thread thread(Portal portal, ThreadStatus status) {
        return em.persist(Thread.builder().slug("t-" + System.nanoTime()).surface(ThreadSurface.DISCUSSION)
                .title("portal query test").status(status).authorId(1L).portalId(portal.getId()).build());
    }

    private Comment comment(Thread thread, CommentStatus status) {
        return em.persist(Comment.builder().subjectType(CommentSubjectType.THREAD).subjectId(thread.getId())
                .body("yorum").status(status).authorId(1L).build());
    }

    // created_at @CreatedDate ile persist anında ezilir (updatable=false): yaş native UPDATE ile verilir.
    private void age(String table, Long id, int days) {
        em.flush();
        em.getEntityManager().createNativeQuery("UPDATE " + table + " SET created_at = :ts WHERE id = :id")
                .setParameter("ts", LocalDateTime.now().minusDays(days)).setParameter("id", id).executeUpdate();
    }

    private List<String> order(String sort) {
        em.flush();
        em.clear();
        PageResponse<PortalSummaryResponse> page = service.list(sort, token, null, PageRequest.of(0, 50));
        return page.content().stream().map(p -> p.slug().substring(token.length() + 1)).toList();
    }

    // ---- trending (kabul kriteri 6) ----

    @Test
    void trending_scoreIsThreeTimesNewThreadsPlusComments_andChangesAsActivityLeavesTheSevenDayWindow() {
        Portal a = portal("a", PortalStatus.ACTIVE, 10, 0);
        Portal b = portal("b", PortalStatus.ACTIVE, 20, 0);
        Portal c = portal("c", PortalStatus.ACTIVE, 30, 0);

        // A: 1 yeni thread => skor 3
        thread(a, ThreadStatus.PUBLISHED);
        // B: eski (20 gün) bir thread'e 4 yeni yorum => skor 4 (thread'in kendisi pencere dışı)
        Thread oldThread = thread(b, ThreadStatus.PUBLISHED);
        age("thread", oldThread.getId(), 20);
        List<Comment> bComments = List.of(comment(oldThread, CommentStatus.PUBLISHED), comment(oldThread, CommentStatus.PUBLISHED),
                comment(oldThread, CommentStatus.PUBLISHED), comment(oldThread, CommentStatus.PUBLISHED));

        assertThat(order("trending")).containsExactly("b", "a", "c"); // 4 > 3 > 0

        // B'nin yorumları 8 gün öncesine kayar (pencere dışı) => B 0'a düşer, A öne geçer; 0'da sortOrder'a göre b < c
        bComments.forEach(cm -> age("comment", cm.getId(), 8));
        assertThat(order("trending")).containsExactly("a", "b", "c");

        // C'ye 2 yeni thread => skor 6, herkesi geçer
        thread(c, ThreadStatus.PUBLISHED);
        thread(c, ThreadStatus.PUBLISHED);
        assertThat(order("trending")).containsExactly("c", "a", "b");

        // A'nın thread'i 8 gün önceye kayınca A da 0'a düşer
        em.flush();
        Long aThreadId = (Long) em.getEntityManager()
                .createQuery("SELECT t.id FROM Thread t WHERE t.portalId = :p").setParameter("p", a.getId())
                .getSingleResult();
        age("thread", aThreadId, 8);
        assertThat(order("trending")).containsExactly("c", "a", "b");
    }

    @Test
    void trending_ignoresDeletedThreadsDeletedCommentsAndCommentsOnDeletedThreads() {
        Portal a = portal("a", PortalStatus.ACTIVE, 10, 0);
        Portal b = portal("b", PortalStatus.ACTIVE, 20, 0);

        thread(a, ThreadStatus.DELETED);                         // silinmiş thread sayılmaz
        Thread live = thread(b, ThreadStatus.PUBLISHED);          // b: 3 puan
        comment(live, CommentStatus.DELETED);                     // silinmiş yorum sayılmaz
        Thread deleted = thread(a, ThreadStatus.DELETED);
        comment(deleted, CommentStatus.PUBLISHED);                // silinmiş thread'in yorumu sayılmaz

        assertThat(order("trending")).containsExactly("b", "a");
        PortalSummaryResponse first = service.list("trending", token, null, PageRequest.of(0, 50)).content().get(0);
        assertThat(first.slug()).endsWith("-b");
    }

    @Test
    void trending_isTheDefaultSort_andUnknownSortFallsBackToIt() {
        Portal a = portal("a", PortalStatus.ACTIVE, 10, 0);
        Portal b = portal("b", PortalStatus.ACTIVE, 20, 0);
        thread(b, ThreadStatus.PUBLISHED);
        em.flush();
        em.clear();

        assertThat(service.list(null, token, null, PageRequest.of(0, 10)).content()).extracting(PortalSummaryResponse::slug)
                .containsExactly(token + "-b", token + "-a");
        assertThat(service.list("saçma", token, null, PageRequest.of(0, 10)).content()).extracting(PortalSummaryResponse::slug)
                .containsExactly(token + "-b", token + "-a");
    }

    @Test
    void trending_pagination_slicesTheSortedSetAndReportsTotals() {
        for (int i = 0; i < 5; i++) {
            portal("p" + i, PortalStatus.ACTIVE, i, 0);
        }
        em.flush();
        em.clear();

        PageResponse<PortalSummaryResponse> page1 = service.list("trending", token, null, PageRequest.of(1, 2));

        assertThat(page1.totalElements()).isEqualTo(5);
        assertThat(page1.totalPages()).isEqualTo(3);
        assertThat(page1.content()).extracting(PortalSummaryResponse::slug).containsExactly(token + "-p2", token + "-p3");
    }

    // ---- diğer sıralamalar ----

    @Test
    void members_isDescending_withIdTieBreaker() {
        portal("a", PortalStatus.ACTIVE, 0, 5);
        portal("b", PortalStatus.ACTIVE, 0, 9);
        portal("c", PortalStatus.ACTIVE, 0, 5);

        assertThat(order("members")).containsExactly("b", "a", "c"); // eşitlikte id artan
    }

    @Test
    void new_isNewestFirst_withIdDescTieBreaker() {
        Portal a = portal("a", PortalStatus.ACTIVE, 0, 0);
        Portal b = portal("b", PortalStatus.ACTIVE, 0, 0);
        Portal c = portal("c", PortalStatus.ACTIVE, 0, 0);
        age("portal", a.getId(), 1);
        age("portal", b.getId(), 3);
        age("portal", c.getId(), 2);

        assertThat(order("new")).containsExactly("a", "c", "b");
    }

    @Test
    void alpha_usesTurkishNameForTrAndEnglishNameOtherwise() {
        em.persist(Portal.builder().slug(token + "-x").nameTr(token + " a").nameEn(token + " z").build());
        em.persist(Portal.builder().slug(token + "-y").nameTr(token + " b").nameEn(token + " y").build());

        LocaleContextHolder.setLocale(Locale.forLanguageTag("tr"));
        assertThat(order("alpha")).containsExactly("x", "y");   // tr: a < b
        LocaleContextHolder.setLocale(Locale.ENGLISH);
        assertThat(order("alpha")).containsExactly("y", "x");   // en: y < z
    }

    // ---- görünürlük / arama ----

    @Test
    void list_showsActiveAndArchived_neverHidden() {
        portal("a", PortalStatus.ACTIVE, 1, 0);
        portal("b", PortalStatus.ARCHIVED, 2, 0);
        portal("c", PortalStatus.HIDDEN, 3, 0);

        for (String sort : List.of("trending", "members", "new", "alpha")) {
            assertThat(order(sort)).as(sort).containsExactlyInAnyOrder("a", "b");
        }
    }

    @Test
    void q_matchesTrAndEnNamesCaseInsensitively_andEscapesWildcards() {
        em.persist(Portal.builder().slug(token + "-tr").nameTr(token + " Sadece Türkçe").nameEn("plain en " + token).build());
        em.persist(Portal.builder().slug(token + "-pct").nameTr(token + " 100% orijinal").nameEn(token + " pct").build());
        em.persist(Portal.builder().slug(token + "-und").nameTr(token + " a_b").nameEn(token + " und").build());
        em.persist(Portal.builder().slug(token + "-oth").nameTr(token + " axb").nameEn(token + " oth").build());
        em.flush();
        em.clear();

        assertThat(slugs(service.list("alpha", token.toUpperCase() + " SADECE", null, PageRequest.of(0, 20))))
                .containsExactly(token + "-tr");                                   // TR kolonu, büyük/küçük harf duyarsız
        assertThat(slugs(service.list("alpha", "plain EN " + token, null, PageRequest.of(0, 20))))
                .containsExactly(token + "-tr");                                   // EN kolonu
        assertThat(slugs(service.list("alpha", token + " 100%", null, PageRequest.of(0, 20))))
                .containsExactly(token + "-pct");                                  // '%' literal (wildcard değil)
        assertThat(slugs(service.list("alpha", token + " a_b", null, PageRequest.of(0, 20))))
                .containsExactly(token + "-und");                                  // '_' literal: "axb" eşleşmez
        assertThat(slugs(service.list("alpha", "%", null, PageRequest.of(0, 20)))).doesNotContain(token + "-tr")
                .as("'%' tek başına her şeyi eşlememeli (yalnız literal % içerenler)");
    }

    @Test
    void q_tooLong_is400_blankMeansNoFilter() {
        assertThatThrownBy(() -> service.list("trending", "x".repeat(101), null, PageRequest.of(0, 20)))
                .isInstanceOf(InvalidReferenceException.class);
        assertThat(ServiceSupport.likePattern("  ")).isEqualTo("%");
        assertThat(ServiceSupport.likePattern("50%_!")).isEqualTo("%50!%!_!!%");
    }

    private static List<String> slugs(PageResponse<PortalSummaryResponse> page) {
        return page.content().stream().map(PortalSummaryResponse::slug).toList();
    }

    // package-private static'e erişim için ince yardımcı
    private static final class ServiceSupport {
        static String likePattern(String q) {
            return PortalQueryServiceImpl.likePattern(q);
        }
    }

    // ---- isMember / productionSlugs / detail ----

    @Test
    void isMember_isFalseForAnonymous_trueOnlyForViewersOwnMemberships_andProductionSlugsAreBatched() {
        Portal a = portal("a", PortalStatus.ACTIVE, 1, 0);
        Portal b = portal("b", PortalStatus.ACTIVE, 2, 0);
        String prod1 = "zq-prod-1-" + System.nanoTime();
        String prod2 = "zq-prod-2-" + System.nanoTime();
        em.persist(PortalProduction.builder().portalId(a.getId()).productionType(PortalProductionType.SERIES).productionSlug(prod2).build());
        em.persist(PortalProduction.builder().portalId(a.getId()).productionType(PortalProductionType.MOVIE).productionSlug(prod1).build());
        em.persist(PortalMembership.builder().portalId(a.getId()).userId(4242L).joinedAt(LocalDateTime.now()).build());
        em.persist(PortalMembership.builder().portalId(b.getId()).userId(9999L).joinedAt(LocalDateTime.now()).build());
        em.flush();
        em.clear();

        List<PortalSummaryResponse> anon = service.list("alpha", token, null, PageRequest.of(0, 20)).content();
        List<PortalSummaryResponse> viewer = service.list("alpha", token, 4242L, PageRequest.of(0, 20)).content();

        assertThat(anon).extracting(PortalSummaryResponse::isMember).containsOnly(false);
        assertThat(viewer).extracting(PortalSummaryResponse::slug, PortalSummaryResponse::isMember)
                .containsExactly(org.assertj.core.groups.Tuple.tuple(token + "-a", true), org.assertj.core.groups.Tuple.tuple(token + "-b", false));
        assertThat(viewer.get(0).productionSlugs()).containsExactly(prod1, prod2);
        assertThat(viewer.get(1).productionSlugs()).isEmpty();
    }

    @Test
    void detail_returnsCreatedAtAndMembership_hiddenAndMissingAre404_archivedIsReadable() {
        Portal a = portal("a", PortalStatus.ARCHIVED, 1, 3);
        Portal hidden = portal("h", PortalStatus.HIDDEN, 2, 0);
        em.persist(PortalMembership.builder().portalId(a.getId()).userId(7L).joinedAt(LocalDateTime.now()).build());
        em.flush();
        em.clear();

        PortalDetailResponse asMember = service.getBySlug(a.getSlug(), 7L);
        PortalDetailResponse anon = service.getBySlug(a.getSlug(), null);

        assertThat(asMember.isMember()).isTrue();
        assertThat(anon.isMember()).isFalse();
        assertThat(anon.status()).isEqualTo(PortalStatus.ARCHIVED);
        assertThat(anon.memberCount()).isEqualTo(3);
        assertThat(anon.createdAt()).isNotNull();
        assertThatThrownBy(() -> service.getBySlug(hidden.getSlug(), null)).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.getBySlug("yok-" + token, null)).isInstanceOf(ResourceNotFoundException.class);
    }

    // ---- Portallarım (GET /api/me/portals) ----

    private PortalMembership joined(Portal p, long userId, int daysAgo) {
        return em.persist(PortalMembership.builder().portalId(p.getId()).userId(userId)
                .joinedAt(LocalDateTime.now().minusDays(daysAgo)).build());
    }

    @Test
    void listJoined_isJoinedAtDescending_archivedVisible_hiddenNot_allMarkedMember() {
        Portal a = portal("a", PortalStatus.ACTIVE, 1, 0);
        Portal archived = portal("b", PortalStatus.ARCHIVED, 2, 0);
        Portal hidden = portal("c", PortalStatus.HIDDEN, 3, 0);
        Portal notJoined = portal("d", PortalStatus.ACTIVE, 4, 0);
        joined(a, 31L, 5);
        joined(archived, 31L, 1);
        joined(hidden, 31L, 0);
        joined(notJoined, 32L, 0);       // başka kullanıcının üyeliği
        em.flush();
        em.clear();

        List<PortalSummaryResponse> mine = service.listJoined(31L, null);

        assertThat(mine).extracting(PortalSummaryResponse::slug).containsExactly(archived.getSlug(), a.getSlug());
        assertThat(mine).extracting(PortalSummaryResponse::isMember).containsOnly(true);
        assertThat(mine.get(0).status()).isEqualTo(PortalStatus.ARCHIVED);
        assertThat(service.listJoined(999_999_777L, null)).isEmpty();
    }

    @Test
    void listJoined_activitySort_followsTrendingScore_tiesKeepJoinedAtOrder() {
        Portal quiet = portal("q", PortalStatus.ACTIVE, 1, 0);
        Portal busy = portal("b", PortalStatus.ACTIVE, 2, 0);
        Portal quiet2 = portal("r", PortalStatus.ACTIVE, 3, 0);
        joined(quiet, 41L, 1);            // en yeni üyelik
        joined(busy, 41L, 9);
        joined(quiet2, 41L, 5);
        thread(busy, ThreadStatus.PUBLISHED);
        em.flush();
        em.clear();

        assertThat(service.listJoined(41L, null)).extracting(PortalSummaryResponse::slug)
                .containsExactly(quiet.getSlug(), quiet2.getSlug(), busy.getSlug());
        assertThat(service.listJoined(41L, "activity")).extracting(PortalSummaryResponse::slug)
                .containsExactly(busy.getSlug(), quiet.getSlug(), quiet2.getSlug());   // eşit skorda joinedAt sırası
    }
}
