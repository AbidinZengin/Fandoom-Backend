package com.example.fandoom_backend.blog.repository;

import com.example.fandoom_backend.blog.entity.Blog;
import com.example.fandoom_backend.blog.entity.BlogRelation;
import com.example.fandoom_backend.blog.entity.BlogStatus;
import com.example.fandoom_backend.blog.entity.BlogTag;
import com.example.fandoom_backend.blog.entity.SubjectType;
import com.example.fandoom_backend.common.config.JpaAuditingConfig;
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
class BlogRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;
    @Autowired
    private BlogRepository blogRepository;
    @Autowired
    private BlogTagRepository blogTagRepository;
    @Autowired
    private BlogRelationRepository blogRelationRepository;

    @Test
    void findBySlugAndStatus_onlyMatchesGivenStatus() {
        Blog draft = persistBlog("cd-draft-slug", BlogStatus.DRAFT);
        entityManager.flush();

        assertThat(blogRepository.findBySlugAndStatus("cd-draft-slug", BlogStatus.PUBLISHED))
                .isEmpty();
        assertThat(blogRepository.findBySlugAndStatus("cd-draft-slug", BlogStatus.DRAFT))
                .map(Blog::getId).contains(draft.getId());
    }

    @Test
    void existsBySlugAndIdNot_excludesOwnIdOnly() {
        Blog blog = persistBlog("cd-unique-slug", BlogStatus.DRAFT);
        entityManager.flush();

        assertThat(blogRepository.existsBySlugAndIdNot("cd-unique-slug", blog.getId())).isFalse();
        assertThat(blogRepository.existsBySlugAndIdNot("cd-unique-slug", -1L)).isTrue();
    }

    @Test
    void incrementViewCount_incrementsPersistedValue() {
        Blog blog = persistBlog("cd-view-count-slug", BlogStatus.PUBLISHED);
        blog.setViewCount(4L);
        entityManager.flush();

        blogRepository.incrementViewCount(blog.getId());
        entityManager.flush();
        entityManager.clear();

        assertThat(blogRepository.findById(blog.getId()).orElseThrow().getViewCount()).isEqualTo(5L);
    }

    @Test
    void findByStatusOrderByPublishedAtDescViewCountDesc_ordersByPublishedAtDesc() {
        Blog older = persistBlog("cd-older", BlogStatus.PUBLISHED);
        older.setPublishedAt(LocalDateTime.now().minusDays(2));
        Blog newer = persistBlog("cd-newer", BlogStatus.PUBLISHED);
        newer.setPublishedAt(LocalDateTime.now().minusDays(1));
        entityManager.flush();

        List<Blog> result = blogRepository.findByStatusOrderByPublishedAtDescViewCountDesc(
                BlogStatus.PUBLISHED, PageRequest.of(0, 50));

        assertThat(result).extracting(Blog::getId).containsSubsequence(newer.getId(), older.getId());
    }

    @Test
    void findByStatusAndIdNotInOrderByPublishedAtDescViewCountDesc_excludesGivenIds() {
        Blog excluded = persistBlog("cd-excluded", BlogStatus.PUBLISHED);
        Blog included = persistBlog("cd-included", BlogStatus.PUBLISHED);
        entityManager.flush();

        List<Blog> result = blogRepository.findByStatusAndIdNotInOrderByPublishedAtDescViewCountDesc(
                BlogStatus.PUBLISHED, List.of(excluded.getId()), PageRequest.of(0, 50));

        assertThat(result).extracting(Blog::getId)
                .contains(included.getId())
                .doesNotContain(excluded.getId());
    }

    @Test
    void findByExactEpisode_onlyMatchesExactEpisodeTaggedAndPublished() {
        Blog published = persistBlog("cd-exact-published", BlogStatus.PUBLISHED);
        persistTag(published, SubjectType.SERIES, 900L, 1, 1, null);

        Blog draft = persistBlog("cd-exact-draft", BlogStatus.DRAFT);
        persistTag(draft, SubjectType.SERIES, 900L, 1, 1, null);

        Blog seasonOnly = persistBlog("cd-season-only", BlogStatus.PUBLISHED);
        persistTag(seasonOnly, SubjectType.SERIES, 900L, 1, null, null);
        entityManager.flush();

        List<Blog> result = blogTagRepository.findByExactEpisode(SubjectType.SERIES, 900L, 1, 1);

        assertThat(result).extracting(Blog::getId)
                .contains(published.getId())
                .doesNotContain(draft.getId(), seasonOnly.getId());
    }

    @Test
    void findBySameSeason_onlyMatchesSeasonLevelTags() {
        Blog seasonLevel = persistBlog("cd-season-level", BlogStatus.PUBLISHED);
        persistTag(seasonLevel, SubjectType.SERIES, 901L, 2, null, null);

        Blog episodeLevel = persistBlog("cd-episode-level", BlogStatus.PUBLISHED);
        persistTag(episodeLevel, SubjectType.SERIES, 901L, 2, 3, null);
        entityManager.flush();

        List<Blog> result = blogTagRepository.findBySameSeason(SubjectType.SERIES, 901L, 2);

        assertThat(result).extracting(Blog::getId)
                .contains(seasonLevel.getId())
                .doesNotContain(episodeLevel.getId());
    }

    @Test
    void findBySameProduction_onlyMatchesProductionLevelTagsForThatSubjectType() {
        Blog productionLevel = persistBlog("cd-production-level", BlogStatus.PUBLISHED);
        persistTag(productionLevel, SubjectType.SERIES, 902L, null, null, null);

        Blog seasonLevel = persistBlog("cd-has-season-902", BlogStatus.PUBLISHED);
        persistTag(seasonLevel, SubjectType.SERIES, 902L, 1, null, null);
        entityManager.flush();

        List<Blog> result = blogTagRepository.findBySameProduction(SubjectType.SERIES, 902L);

        assertThat(result).extracting(Blog::getId)
                .contains(productionLevel.getId())
                .doesNotContain(seasonLevel.getId());
        // Aynı subjectId farklı subjectType'ta yanlışlıkla eşleşmemeli.
        assertThat(blogTagRepository.findBySameProduction(SubjectType.MOVIE, 902L))
                .doesNotContain(productionLevel);
    }

    @Test
    void findBySameFranchise_matchesFranchiseOnlyTagsAndPublishedOnly() {
        Blog published = persistBlog("cd-franchise-published", BlogStatus.PUBLISHED);
        persistTag(published, null, null, null, null, 555L);

        Blog draft = persistBlog("cd-franchise-draft", BlogStatus.DRAFT);
        persistTag(draft, null, null, null, null, 555L);

        Blog otherFranchise = persistBlog("cd-other-franchise", BlogStatus.PUBLISHED);
        persistTag(otherFranchise, null, null, null, null, 556L);
        entityManager.flush();

        List<Blog> result = blogTagRepository.findBySameFranchise(555L);

        assertThat(result).extracting(Blog::getId)
                .contains(published.getId())
                .doesNotContain(draft.getId(), otherFranchise.getId());
    }

    @Test
    void findBySourceBlogIdOrderByOrderIndexAsc_returnsOrderedRelations() {
        Blog source = persistBlog("cd-relation-source", BlogStatus.PUBLISHED);
        Blog first = persistBlog("cd-relation-first", BlogStatus.PUBLISHED);
        Blog second = persistBlog("cd-relation-second", BlogStatus.PUBLISHED);

        entityManager.persist(BlogRelation.builder()
                .sourceBlog(source).relatedBlog(second).orderIndex(1).build());
        entityManager.persist(BlogRelation.builder()
                .sourceBlog(source).relatedBlog(first).orderIndex(0).build());
        entityManager.flush();
        entityManager.clear();

        List<BlogRelation> result =
                blogRelationRepository.findBySourceBlogIdOrderByOrderIndexAsc(source.getId());

        assertThat(result).extracting(r -> r.getRelatedBlog().getId())
                .containsExactly(first.getId(), second.getId());
    }

    private Blog persistBlog(String slug, BlogStatus status) {
        Blog blog = Blog.builder()
                .title(slug)
                .slug(slug)
                .status(status)
                .build();
        return entityManager.persist(blog);
    }

    private BlogTag persistTag(Blog blog, SubjectType subjectType, Long subjectId,
                                Integer seasonNumber, Integer episodeNumber, Long franchiseId) {
        BlogTag tag = BlogTag.builder()
                .blog(blog)
                .subjectType(subjectType)
                .subjectId(subjectId)
                .seasonNumber(seasonNumber)
                .episodeNumber(episodeNumber)
                .franchiseId(franchiseId)
                .build();
        return entityManager.persist(tag);
    }
}
