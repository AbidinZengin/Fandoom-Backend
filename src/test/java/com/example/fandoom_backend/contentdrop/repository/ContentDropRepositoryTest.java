package com.example.fandoom_backend.contentdrop.repository;

import com.example.fandoom_backend.common.config.JpaAuditingConfig;
import com.example.fandoom_backend.contentdrop.entity.ContentDrop;
import com.example.fandoom_backend.contentdrop.entity.ContentDropRelation;
import com.example.fandoom_backend.contentdrop.entity.ContentDropStatus;
import com.example.fandoom_backend.contentdrop.entity.ContentDropTag;
import com.example.fandoom_backend.contentdrop.entity.SubjectType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// Ayrı bir test veritabanı (H2/Testcontainers) bu projede henüz yok — mevcut
// FandoomBackendApplicationTests gibi gerçek yerel MySQL'e karşı çalışır.
// @DataJpaTest her test metodunu kendi transaction'ında rollback eder, dev
// DB'de kalıcı iz bırakmaz.
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaAuditingConfig.class)
class ContentDropRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;
    @Autowired
    private ContentDropRepository contentDropRepository;
    @Autowired
    private ContentDropTagRepository contentDropTagRepository;
    @Autowired
    private ContentDropRelationRepository contentDropRelationRepository;

    @Test
    void findBySlugAndStatus_onlyMatchesGivenStatus() {
        ContentDrop draft = persistContentDrop("cd-draft-slug", ContentDropStatus.DRAFT);
        entityManager.flush();

        assertThat(contentDropRepository.findBySlugAndStatus("cd-draft-slug", ContentDropStatus.PUBLISHED))
                .isEmpty();
        assertThat(contentDropRepository.findBySlugAndStatus("cd-draft-slug", ContentDropStatus.DRAFT))
                .map(ContentDrop::getId).contains(draft.getId());
    }

    @Test
    void existsBySlugAndIdNot_excludesOwnIdOnly() {
        ContentDrop contentDrop = persistContentDrop("cd-unique-slug", ContentDropStatus.DRAFT);
        entityManager.flush();

        assertThat(contentDropRepository.existsBySlugAndIdNot("cd-unique-slug", contentDrop.getId())).isFalse();
        assertThat(contentDropRepository.existsBySlugAndIdNot("cd-unique-slug", -1L)).isTrue();
    }

    @Test
    void incrementViewCount_incrementsPersistedValue() {
        ContentDrop contentDrop = persistContentDrop("cd-view-count-slug", ContentDropStatus.PUBLISHED);
        contentDrop.setViewCount(4L);
        entityManager.flush();

        contentDropRepository.incrementViewCount(contentDrop.getId());
        entityManager.flush();
        entityManager.clear();

        assertThat(contentDropRepository.findById(contentDrop.getId()).orElseThrow().getViewCount()).isEqualTo(5L);
    }

    @Test
    void findByStatusOrderByPublishedAtDescViewCountDesc_ordersByPublishedAtDesc() {
        ContentDrop older = persistContentDrop("cd-older", ContentDropStatus.PUBLISHED);
        older.setPublishedAt(LocalDateTime.now().minusDays(2));
        ContentDrop newer = persistContentDrop("cd-newer", ContentDropStatus.PUBLISHED);
        newer.setPublishedAt(LocalDateTime.now().minusDays(1));
        entityManager.flush();

        List<ContentDrop> result = contentDropRepository.findByStatusOrderByPublishedAtDescViewCountDesc(
                ContentDropStatus.PUBLISHED, PageRequest.of(0, 50));

        assertThat(result).extracting(ContentDrop::getId).containsSubsequence(newer.getId(), older.getId());
    }

    @Test
    void findByStatusAndIdNotInOrderByPublishedAtDescViewCountDesc_excludesGivenIds() {
        ContentDrop excluded = persistContentDrop("cd-excluded", ContentDropStatus.PUBLISHED);
        ContentDrop included = persistContentDrop("cd-included", ContentDropStatus.PUBLISHED);
        entityManager.flush();

        List<ContentDrop> result = contentDropRepository.findByStatusAndIdNotInOrderByPublishedAtDescViewCountDesc(
                ContentDropStatus.PUBLISHED, List.of(excluded.getId()), PageRequest.of(0, 50));

        assertThat(result).extracting(ContentDrop::getId)
                .contains(included.getId())
                .doesNotContain(excluded.getId());
    }

    @Test
    void findByExactEpisode_onlyMatchesExactEpisodeTaggedAndPublished() {
        ContentDrop published = persistContentDrop("cd-exact-published", ContentDropStatus.PUBLISHED);
        persistTag(published, SubjectType.SERIES, 900L, 1, 1, null);

        ContentDrop draft = persistContentDrop("cd-exact-draft", ContentDropStatus.DRAFT);
        persistTag(draft, SubjectType.SERIES, 900L, 1, 1, null);

        ContentDrop seasonOnly = persistContentDrop("cd-season-only", ContentDropStatus.PUBLISHED);
        persistTag(seasonOnly, SubjectType.SERIES, 900L, 1, null, null);
        entityManager.flush();

        List<ContentDrop> result = contentDropTagRepository.findByExactEpisode(SubjectType.SERIES, 900L, 1, 1);

        assertThat(result).extracting(ContentDrop::getId)
                .contains(published.getId())
                .doesNotContain(draft.getId(), seasonOnly.getId());
    }

    @Test
    void findBySameSeason_onlyMatchesSeasonLevelTags() {
        ContentDrop seasonLevel = persistContentDrop("cd-season-level", ContentDropStatus.PUBLISHED);
        persistTag(seasonLevel, SubjectType.SERIES, 901L, 2, null, null);

        ContentDrop episodeLevel = persistContentDrop("cd-episode-level", ContentDropStatus.PUBLISHED);
        persistTag(episodeLevel, SubjectType.SERIES, 901L, 2, 3, null);
        entityManager.flush();

        List<ContentDrop> result = contentDropTagRepository.findBySameSeason(SubjectType.SERIES, 901L, 2);

        assertThat(result).extracting(ContentDrop::getId)
                .contains(seasonLevel.getId())
                .doesNotContain(episodeLevel.getId());
    }

    @Test
    void findBySameProduction_onlyMatchesProductionLevelTagsForThatSubjectType() {
        ContentDrop productionLevel = persistContentDrop("cd-production-level", ContentDropStatus.PUBLISHED);
        persistTag(productionLevel, SubjectType.SERIES, 902L, null, null, null);

        ContentDrop seasonLevel = persistContentDrop("cd-has-season-902", ContentDropStatus.PUBLISHED);
        persistTag(seasonLevel, SubjectType.SERIES, 902L, 1, null, null);
        entityManager.flush();

        List<ContentDrop> result = contentDropTagRepository.findBySameProduction(SubjectType.SERIES, 902L);

        assertThat(result).extracting(ContentDrop::getId)
                .contains(productionLevel.getId())
                .doesNotContain(seasonLevel.getId());
        // Aynı subjectId farklı subjectType'ta yanlışlıkla eşleşmemeli.
        assertThat(contentDropTagRepository.findBySameProduction(SubjectType.MOVIE, 902L))
                .doesNotContain(productionLevel);
    }

    @Test
    void findBySameFranchise_matchesFranchiseOnlyTagsAndPublishedOnly() {
        ContentDrop published = persistContentDrop("cd-franchise-published", ContentDropStatus.PUBLISHED);
        persistTag(published, null, null, null, null, 555L);

        ContentDrop draft = persistContentDrop("cd-franchise-draft", ContentDropStatus.DRAFT);
        persistTag(draft, null, null, null, null, 555L);

        ContentDrop otherFranchise = persistContentDrop("cd-other-franchise", ContentDropStatus.PUBLISHED);
        persistTag(otherFranchise, null, null, null, null, 556L);
        entityManager.flush();

        List<ContentDrop> result = contentDropTagRepository.findBySameFranchise(555L);

        assertThat(result).extracting(ContentDrop::getId)
                .contains(published.getId())
                .doesNotContain(draft.getId(), otherFranchise.getId());
    }

    @Test
    void findBySourceContentDropIdOrderByOrderIndexAsc_returnsOrderedRelations() {
        ContentDrop source = persistContentDrop("cd-relation-source", ContentDropStatus.PUBLISHED);
        ContentDrop first = persistContentDrop("cd-relation-first", ContentDropStatus.PUBLISHED);
        ContentDrop second = persistContentDrop("cd-relation-second", ContentDropStatus.PUBLISHED);

        entityManager.persist(ContentDropRelation.builder()
                .sourceContentDrop(source).relatedContentDrop(second).orderIndex(1).build());
        entityManager.persist(ContentDropRelation.builder()
                .sourceContentDrop(source).relatedContentDrop(first).orderIndex(0).build());
        entityManager.flush();
        entityManager.clear();

        List<ContentDropRelation> result =
                contentDropRelationRepository.findBySourceContentDropIdOrderByOrderIndexAsc(source.getId());

        assertThat(result).extracting(r -> r.getRelatedContentDrop().getId())
                .containsExactly(first.getId(), second.getId());
    }

    private ContentDrop persistContentDrop(String slug, ContentDropStatus status) {
        ContentDrop contentDrop = ContentDrop.builder()
                .title(slug)
                .slug(slug)
                .status(status)
                .build();
        return entityManager.persist(contentDrop);
    }

    private ContentDropTag persistTag(ContentDrop contentDrop, SubjectType subjectType, Long subjectId,
                                       Integer seasonNumber, Integer episodeNumber, Long franchiseId) {
        ContentDropTag tag = ContentDropTag.builder()
                .contentDrop(contentDrop)
                .subjectType(subjectType)
                .subjectId(subjectId)
                .seasonNumber(seasonNumber)
                .episodeNumber(episodeNumber)
                .franchiseId(franchiseId)
                .build();
        return entityManager.persist(tag);
    }
}
