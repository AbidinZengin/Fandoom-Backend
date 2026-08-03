package com.example.fandoom_backend.blog.sort;

import com.example.fandoom_backend.blog.entity.Blog;
import com.example.fandoom_backend.blog.entity.BlogStatus;
import com.example.fandoom_backend.blog.repository.BlogRepository;
import com.example.fandoom_backend.common.config.JpaAuditingConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// Sort.Order#nullsLast()'ın gerçekten "ORDER BY ... NULLS LAST" olarak SQL'e
// çevrildiğini (ve MySQL'in varsayılan NULL-küçük davranışını EZDİĞİNİ)
// doğrulamak, standart bir Mockito birim testiyle mümkün değil — gerçek bir
// sorguya karşı çalıştırılması gerekiyor.
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaAuditingConfig.class)
class RecommendedBlogSortStrategyDataJpaTest {

    @Autowired
    private TestEntityManager entityManager;
    @Autowired
    private BlogRepository blogRepository;

    private final RecommendedBlogSortStrategy strategy = new RecommendedBlogSortStrategy();

    @Test
    void toJpaSort_blogsWithNullRecommendedRank_sortLastRegardlessOfMySqlDefaultNullOrdering() {
        Blog withoutRank = persistBlog("rb-no-rank", null);
        Blog rankTwo = persistBlog("rb-rank-two", 2);
        Blog rankOne = persistBlog("rb-rank-one", 1);
        entityManager.flush();

        List<Long> ids = blogRepository.findAll(strategy.toJpaSort()).stream()
                .map(Blog::getId)
                .filter(id -> id.equals(withoutRank.getId())
                        || id.equals(rankOne.getId())
                        || id.equals(rankTwo.getId()))
                .toList();

        assertThat(ids).containsExactly(rankOne.getId(), rankTwo.getId(), withoutRank.getId());
    }

    private Blog persistBlog(String slug, Integer recommendedRank) {
        Blog blog = Blog.builder()
                .title(slug)
                .slug(slug)
                .status(BlogStatus.DRAFT)
                .recommendedRank(recommendedRank)
                .build();
        return entityManager.persist(blog);
    }
}
