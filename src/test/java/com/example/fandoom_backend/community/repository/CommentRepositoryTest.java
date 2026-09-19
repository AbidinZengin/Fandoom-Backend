package com.example.fandoom_backend.community.repository;

import com.example.fandoom_backend.common.config.JpaAuditingConfig;
import com.example.fandoom_backend.common.specification.KeysetSpecification;
import com.example.fandoom_backend.community.entity.Comment;
import com.example.fandoom_backend.community.entity.CommentStatus;
import com.example.fandoom_backend.community.entity.CommentSubjectType;
import com.example.fandoom_backend.community.specification.CommentSpecificationBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

// BlogRepositoryTest ile aynı desen: gerçek yerel MySQL'e karşı, her test rollback edilir.
// Bu testler yalnızca gerçek DB'de doğrulanabilen iki sorguyu kapsar: MySQL 8 pencere
// fonksiyonlu toplu yanıt önizlemesi (native) ve keyset (cursor) Specification'ı.
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaAuditingConfig.class)
class CommentRepositoryTest {

    // Gerçek verilerle çakışmasın diye uydurma bir konu id'si.
    private static final Long SUBJECT_ID = 987_654_321L;

    @Autowired
    private TestEntityManager entityManager;
    @Autowired
    private CommentRepository commentRepository;

    @Test
    void findFirstRepliesByParentIds_returnsAtMostLimitPerParent_andCountsAreGrouped() {
        Comment p1 = persist(null, 0);
        Comment p2 = persist(null, 0);
        for (int i = 0; i < 5; i++) {
            persist(p1, 0);
        }
        persist(p2, 0);
        entityManager.flush();
        entityManager.clear();

        List<Comment> replies = commentRepository.findFirstRepliesByParentIds(List.of(p1.getId(), p2.getId()), 3);
        Map<Long, Long> perParent = replies.stream()
                .collect(Collectors.groupingBy(r -> r.getParent().getId(), Collectors.counting()));

        assertThat(perParent).containsEntry(p1.getId(), 3L).containsEntry(p2.getId(), 1L);

        Map<Long, Long> counts = commentRepository.countRepliesByParentIds(List.of(p1.getId(), p2.getId())).stream()
                .collect(Collectors.toMap(r -> (Long) r[0], r -> (Long) r[1]));
        assertThat(counts).containsEntry(p1.getId(), 5L).containsEntry(p2.getId(), 1L);
    }

    @Test
    void keysetAfterCursor_returnsOnlyRowsStrictlyAfterInSortOrder_withoutCount() {
        Comment top = persist(null, 5);
        Comment mid = persist(null, 3);
        Comment low = persist(null, 1);
        persist(mid, 99); // yanıt: üst seviye listesine girmemeli
        entityManager.flush();
        entityManager.clear();

        Sort order = Sort.by(Sort.Direction.DESC, "likeCount").and(Sort.by(Sort.Direction.DESC, "id"));
        Specification<Comment> base = CommentSpecificationBuilder.topLevelOf(CommentSubjectType.BLOG, SUBJECT_ID);

        List<Comment> firstPage = commentRepository.findBy(base, q -> q.sortBy(order).limit(3).all());
        assertThat(firstPage).extracting(Comment::getId).containsExactly(top.getId(), mid.getId(), low.getId());

        Specification<Comment> afterMid = base.and(
                KeysetSpecification.<Comment, Integer>after("likeCount", 3, mid.getId()));
        List<Comment> nextPage = commentRepository.findBy(afterMid, q -> q.sortBy(order).limit(3).all());
        assertThat(nextPage).extracting(Comment::getId).containsExactly(low.getId());
    }

    private Comment persist(Comment parent, int likeCount) {
        return entityManager.persist(Comment.builder()
                .subjectType(CommentSubjectType.BLOG)
                .subjectId(SUBJECT_ID)
                .parent(parent)
                .body("test yorumu")
                .status(CommentStatus.PUBLISHED)
                .authorId(1L)
                .likeCount(likeCount)
                .build());
    }
}
