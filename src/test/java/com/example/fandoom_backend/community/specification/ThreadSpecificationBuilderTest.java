package com.example.fandoom_backend.community.specification;

import com.example.fandoom_backend.common.config.JpaAuditingConfig;
import com.example.fandoom_backend.community.entity.Portal;
import com.example.fandoom_backend.community.entity.PortalStatus;
import com.example.fandoom_backend.community.entity.Thread;
import com.example.fandoom_backend.community.entity.ThreadStatus;
import com.example.fandoom_backend.community.entity.ThreadSurface;
import com.example.fandoom_backend.community.entity.ThreadTag;
import com.example.fandoom_backend.community.repository.ThreadRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// hasTagsIn Java tarafında ID biriktirmek yerine gerçek JPA Criteria EXISTS alt sorgusu ürettiği için
// (bkz. ThreadSpecificationBuilder), bunu asıl anlamlı doğrulayan test gerçek bir veritabanına karşı
// çalıştırmaktır — BlogSpecificationBuilderTest ile aynı desen. Diğer testlerden kalan (rollback
// edilmemiş/committed) satırlarla çakışmayı önlemek için her test kendi nanoTime-bazlı benzersiz tag'ini kullanır.
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaAuditingConfig.class)
class ThreadSpecificationBuilderTest {

    @Autowired
    private TestEntityManager entityManager;
    @Autowired
    private ThreadRepository threadRepository;

    private Thread persistThread(String slugPrefix) {
        Portal portal = entityManager.persist(Portal.builder()
                .slug("sb-portal-" + System.nanoTime()).nameTr("Test").nameEn("Test")
                .status(PortalStatus.ACTIVE).build());
        return entityManager.persist(Thread.builder()
                .slug(slugPrefix + "-" + System.nanoTime()).surface(ThreadSurface.THEORY)
                .title("spec builder test").status(ThreadStatus.PUBLISHED).authorId(1L)
                .portalId(portal.getId()).build());
    }

    private void persistTag(Thread thread, String tag) {
        entityManager.persist(ThreadTag.builder().thread(thread).tag(tag).build());
    }

    private List<Thread> findAll(Specification<Thread> specification) {
        return threadRepository.findAll(specification);
    }

    @Test
    void hasTagsIn_matchesOnlyThreadsWithThatTag() {
        String tag = "sb-tag-" + System.nanoTime();
        Thread matching = persistThread("sb-match");
        persistTag(matching, tag);
        Thread other = persistThread("sb-other");
        persistTag(other, "sb-unrelated-" + System.nanoTime());
        entityManager.flush();
        entityManager.clear();

        List<Thread> result = findAll(ThreadSpecificationBuilder.hasTagsIn(List.of(tag)));

        assertThat(result).extracting(Thread::getId)
                .contains(matching.getId())
                .doesNotContain(other.getId());
    }

    // En kritik test: bir thread'in birden fazla eşleşen tag'i olsa bile EXISTS alt sorgusu sayesinde
    // JOIN+distinct'e gerek kalmadan TEK satır dönmesi gerekiyor.
    @Test
    void hasTagsIn_threadWithMultipleMatchingTags_returnsSingleRowWithoutDuplicates() {
        String tagA = "sb-multi-a-" + System.nanoTime();
        String tagB = "sb-multi-b-" + System.nanoTime();
        Thread multiTag = persistThread("sb-multi");
        persistTag(multiTag, tagA);
        persistTag(multiTag, tagB);
        entityManager.flush();
        entityManager.clear();

        List<Thread> result = findAll(ThreadSpecificationBuilder.hasTagsIn(List.of(tagA, tagB)));

        assertThat(result.stream().filter(t -> t.getId().equals(multiTag.getId())).count()).isEqualTo(1);
    }

    @Test
    void hasTagsIn_emptyList_matchesNoThreads() {
        Thread thread = persistThread("sb-empty");
        persistTag(thread, "sb-irrelevant-" + System.nanoTime());
        entityManager.flush();
        entityManager.clear();

        List<Thread> result = findAll(
                Specification.allOf(ThreadSpecificationBuilder.isPublished(), ThreadSpecificationBuilder.hasTagsIn(List.of())));

        assertThat(result).isEmpty();
    }

    @Test
    void hasTagsIn_null_returnsNullSpecification() {
        assertThat(ThreadSpecificationBuilder.hasTagsIn(null)).isNull();
    }
}
