package com.example.fandoom_backend.community.service;

import com.example.fandoom_backend.common.config.JpaAuditingConfig;
import com.example.fandoom_backend.community.dto.PortalUpdateRequest;
import com.example.fandoom_backend.community.dto.ThreadPatchRequest;
import com.example.fandoom_backend.community.dto.TrendingTagResponse;
import com.example.fandoom_backend.community.entity.Portal;
import com.example.fandoom_backend.community.entity.PortalStatus;
import com.example.fandoom_backend.community.entity.Thread;
import com.example.fandoom_backend.community.entity.ThreadStatus;
import com.example.fandoom_backend.community.entity.ThreadSurface;
import com.example.fandoom_backend.community.entity.ThreadTag;
import com.example.fandoom_backend.community.repository.ThreadTagRepository;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// Güvenlik denetimi düzeltmeleri (gerçek MySQL, her test rollback): HIDDEN portaldaki thread'in tag'i
// trending'de ve takip sayısında görünmez; PATCH doğrulamaları.
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaAuditingConfig.class)
class HiddenPortalLeakRegressionTest {

    @Autowired private TestEntityManager em;
    @Autowired private ThreadTagRepository threadTagRepository;

    private final String token = "lk" + System.nanoTime();

    private Portal portal(String suffix, PortalStatus status) {
        return em.persist(Portal.builder().slug(token + "-" + suffix).nameTr("T").nameEn("E").status(status).build());
    }

    private void taggedThread(Portal portal, String tag) {
        Thread t = em.persist(Thread.builder().slug("t-" + System.nanoTime()).surface(ThreadSurface.THEORY)
                .title("tag leak test").status(ThreadStatus.PUBLISHED).authorId(1L).portalId(portal.getId()).build());
        em.persist(ThreadTag.builder().thread(t).tag(tag).build());
    }

    private List<TrendingTagResponse> trending() {
        em.flush();
        em.clear();
        return threadTagRepository.findTrending(ThreadStatus.PUBLISHED, LocalDateTime.now().minusDays(7), Pageable.ofSize(500));
    }

    @Test
    void hiddenPortalThreadTags_areNotInTrending_activeOnesAre() {
        Portal hidden = portal("hidden", PortalStatus.HIDDEN);
        Portal active = portal("active", PortalStatus.ACTIVE);
        Portal archived = portal("archived", PortalStatus.ARCHIVED);
        taggedThread(hidden, token + "-secret");
        taggedThread(hidden, token + "-shared");
        taggedThread(active, token + "-shared");
        taggedThread(archived, token + "-arch");

        List<TrendingTagResponse> result = trending();

        assertThat(result).extracting(TrendingTagResponse::tag)
                .doesNotContain(token + "-secret")
                .contains(token + "-shared", token + "-arch");
        // ortak tag'in sayısı yalnız görünür portaldaki thread'i sayar (1, HIDDEN'daki değil)
        assertThat(result).filteredOn(t -> t.tag().equals(token + "-shared")).singleElement()
                .satisfies(t -> assertThat(t.threadCount()).isEqualTo(1));
    }

    @Test
    void followedTagCount_excludesHiddenPortalThreads() {
        Portal hidden = portal("hidden", PortalStatus.HIDDEN);
        Portal active = portal("active", PortalStatus.ACTIVE);
        taggedThread(hidden, token + "-t");
        taggedThread(hidden, token + "-t");
        taggedThread(active, token + "-t");
        em.flush();
        em.clear();

        assertThat(threadTagRepository.countByTagAndThread_Status(token + "-t", ThreadStatus.PUBLISHED)).isEqualTo(1);
        assertThat(threadTagRepository.countByTagAndThread_Status(token + "-yok", ThreadStatus.PUBLISHED)).isZero();
    }

    // ---- DTO doğrulamaları ----

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private static PortalUpdateRequest update(String nameTr, String nameEn, String descTr, String descEn) {
        return new PortalUpdateRequest(null, nameTr, nameEn, descTr, descEn, null, null, null, null, null, null, null);
    }

    @Test
    void portalUpdate_blankNamesAndWhitespaceOnlyDescriptionsAreInvalid_nullMeansUnchanged() {
        assertThat(validator.validate(update("   ", null, null, null))).extracting(v -> v.getPropertyPath().toString())
                .containsExactly("nameTr");
        assertThat(validator.validate(update(null, "     ", null, null))).extracting(v -> v.getPropertyPath().toString())
                .containsExactly("nameEn");
        assertThat(validator.validate(update(null, null, "   ", null))).extracting(v -> v.getPropertyPath().toString())
                .containsExactly("descriptionTr");
        assertThat(validator.validate(update(null, null, null, "\t \n"))).extracting(v -> v.getPropertyPath().toString())
                .containsExactly("descriptionEn");

        assertThat(validator.validate(update(null, null, null, null))).isEmpty();                    // hepsi null = değişmedi
        assertThat(validator.validate(update("Yeni Ad", "New Name", "açıklama", "desc"))).isEmpty();
        assertThat(validator.validate(update(null, null, "", ""))).isEmpty();                        // "" açıklamayı temizler
        assertThat(validator.validate(update(" Ad ", null, null, null))).isEmpty();                  // kenar boşluğu serbest (trim'lenir)
    }

    @Test
    void threadPatch_blankPortalSlugBecomesNull_soItCanNeverBeA403Or404Trigger() {
        assertThat(new ThreadPatchRequest(null, null, null, null, null, null, "").portalSlug()).isNull();
        assertThat(new ThreadPatchRequest(null, null, null, null, null, null, "  \t").portalSlug()).isNull();
        assertThat(new ThreadPatchRequest(null, null, null, null, null, null, null).portalSlug()).isNull();
        assertThat(new ThreadPatchRequest(null, null, null, null, null, null, " westeros ").portalSlug()).isEqualTo("westeros");
    }
}
