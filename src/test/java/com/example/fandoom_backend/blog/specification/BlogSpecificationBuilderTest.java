package com.example.fandoom_backend.blog.specification;

import com.example.fandoom_backend.blog.entity.Blog;
import com.example.fandoom_backend.blog.entity.BlogStatus;
import com.example.fandoom_backend.blog.entity.BlogTag;
import com.example.fandoom_backend.blog.repository.BlogRepository;
import com.example.fandoom_backend.common.config.JpaAuditingConfig;
import com.example.fandoom_backend.tag.entity.Tag;
import com.example.fandoom_backend.tag.entity.TagAssignment;
import com.example.fandoom_backend.tag.entity.TagType;
import com.example.fandoom_backend.tag.entity.TaggableType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// BlogSpecificationBuilder gerçek JPA Criteria API kullandığı (subquery+EXISTS)
// için bunu asıl anlamlı doğrulayan test, gerçek bir veritabanına karşı
// çalıştırmaktır. Aynı BlogRepositoryTest konvansiyonu: ayrı bir test DB
// (H2/Testcontainers) henüz yok, yerel MySQL'e karşı çalışır, @DataJpaTest her
// test metodunu kendi transaction'ında rollback eder.
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaAuditingConfig.class)
class BlogSpecificationBuilderTest {

    private static final Pageable PAGE = PageRequest.of(0, 50);

    @Autowired
    private TestEntityManager entityManager;
    @Autowired
    private BlogRepository blogRepository;

    @Test
    void hasFormat_matchesOnlyBlogsWithThatFormatTag() {
        Blog tagged = persistBlog("sb-format-tagged");
        Blog untagged = persistBlog("sb-format-untagged");
        Tag formatTag = persistTag(TagType.FORMAT, "sb-listicle");
        persistAssignment(tagged, formatTag);
        entityManager.flush();

        List<Blog> result = findAll(BlogSpecificationBuilder.hasFormat("sb-listicle"));

        assertThat(result).extracting(Blog::getId)
                .contains(tagged.getId())
                .doesNotContain(untagged.getId());
    }

    // En kritik test: bir blog'un birden fazla eşleşen mood tag'i olsa bile
    // EXISTS alt-sorgusu sayesinde JOIN+distinct'e gerek kalmadan TEK satır
    // dönmesi gerekiyor.
    @Test
    void hasAnyMood_blogWithMultipleMatchingMoods_returnsSingleRowWithoutDuplicates() {
        Blog multiMood = persistBlog("sb-multi-mood");
        Tag dark = persistTag(TagType.MOOD, "sb-dark");
        Tag hopeful = persistTag(TagType.MOOD, "sb-hopeful");
        persistAssignment(multiMood, dark);
        persistAssignment(multiMood, hopeful);
        entityManager.flush();

        List<Blog> result = findAll(BlogSpecificationBuilder.hasAnyMood(List.of("sb-dark", "sb-hopeful")));

        assertThat(result.stream().filter(b -> b.getId().equals(multiMood.getId())).count()).isEqualTo(1);
    }

    @Test
    void hasAnyMood_noMatchingMood_excludesBlog() {
        Blog other = persistBlog("sb-mood-nomatch");
        Tag silly = persistTag(TagType.MOOD, "sb-silly");
        persistAssignment(other, silly);
        entityManager.flush();

        List<Blog> result = findAll(BlogSpecificationBuilder.hasAnyMood(List.of("sb-dark-2")));

        assertThat(result).extracting(Blog::getId).doesNotContain(other.getId());
    }

    @Test
    void hasFranchise_matchesBlogTagWithGivenFranchiseId() {
        Blog matching = persistBlog("sb-franchise-match");
        persistFranchiseTag(matching, 900L);
        Blog other = persistBlog("sb-franchise-other");
        persistFranchiseTag(other, 901L);
        entityManager.flush();

        List<Blog> result = findAll(BlogSpecificationBuilder.hasFranchise(900L));

        assertThat(result).extracting(Blog::getId)
                .contains(matching.getId())
                .doesNotContain(other.getId());
    }

    @Test
    void isSpoilerFree_null_appliesNoFilter() {
        Blog spoilerFree = persistBlog("sb-spoiler-free", true);
        Blog notSpoilerFree = persistBlog("sb-spoiler-not-free", false);
        entityManager.flush();

        List<Blog> result = findAll(BlogSpecificationBuilder.isSpoilerFree(null));

        assertThat(result).extracting(Blog::getId)
                .contains(spoilerFree.getId(), notSpoilerFree.getId());
    }

    @Test
    void isSpoilerFree_true_onlyMatchesSpoilerFreeBlogs() {
        Blog spoilerFree = persistBlog("sb-spoiler-free-2", true);
        Blog notSpoilerFree = persistBlog("sb-spoiler-not-free-2", false);
        entityManager.flush();

        List<Blog> result = findAll(BlogSpecificationBuilder.isSpoilerFree(true));

        assertThat(result).extracting(Blog::getId)
                .contains(spoilerFree.getId())
                .doesNotContain(notSpoilerFree.getId());
    }

    @Test
    void combinedFacets_appliesAndSemantics() {
        Blog matchesBoth = persistBlog("sb-and-both");
        Blog onlyFormat = persistBlog("sb-and-format-only");
        Blog onlyMood = persistBlog("sb-and-mood-only");
        Tag format = persistTag(TagType.FORMAT, "sb-and-listicle");
        Tag mood = persistTag(TagType.MOOD, "sb-and-dark");
        persistAssignment(matchesBoth, format);
        persistAssignment(matchesBoth, mood);
        persistAssignment(onlyFormat, format);
        persistAssignment(onlyMood, mood);
        entityManager.flush();

        List<Blog> result = findAll(Specification.allOf(
                BlogSpecificationBuilder.hasFormat("sb-and-listicle"),
                BlogSpecificationBuilder.hasAnyMood(List.of("sb-and-dark"))));

        assertThat(result).extracting(Blog::getId)
                .contains(matchesBoth.getId())
                .doesNotContain(onlyFormat.getId(), onlyMood.getId());
    }

    private List<Blog> findAll(Specification<Blog> specification) {
        return blogRepository.findAll(specification, PAGE).getContent();
    }

    private Blog persistBlog(String slug) {
        return persistBlog(slug, false);
    }

    private Blog persistBlog(String slug, boolean spoilerFree) {
        Blog blog = Blog.builder()
                .title(slug)
                .slug(slug)
                .status(BlogStatus.PUBLISHED)
                .spoilerFree(spoilerFree)
                .build();
        return entityManager.persist(blog);
    }

    private void persistFranchiseTag(Blog blog, Long franchiseId) {
        entityManager.persist(BlogTag.builder().blog(blog).franchiseId(franchiseId).build());
    }

    private Tag persistTag(TagType type, String slug) {
        return entityManager.persist(Tag.builder().name(slug).slug(slug).type(type).build());
    }

    private void persistAssignment(Blog blog, Tag tag) {
        entityManager.persist(TagAssignment.builder()
                .tag(tag).taggableType(TaggableType.BLOG).taggableId(blog.getId()).build());
    }
}
