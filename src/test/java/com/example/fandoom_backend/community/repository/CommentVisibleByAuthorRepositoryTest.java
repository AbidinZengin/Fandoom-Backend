package com.example.fandoom_backend.community.repository;

import com.example.fandoom_backend.common.config.JpaAuditingConfig;
import com.example.fandoom_backend.community.entity.Comment;
import com.example.fandoom_backend.community.entity.CommentStatus;
import com.example.fandoom_backend.community.entity.CommentSubjectType;
import com.example.fandoom_backend.community.entity.Portal;
import com.example.fandoom_backend.community.entity.PortalStatus;
import com.example.fandoom_backend.community.entity.Thread;
import com.example.fandoom_backend.community.entity.ThreadStatus;
import com.example.fandoom_backend.community.entity.ThreadSurface;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// Gerçek MySQL (diğer @DataJpaTest'lerle aynı desen, her test rollback). Profildeki "Yorumlar" sekmesinin
// süzgeci: silinmiş/gizli içerik profilden sızmamalı. Başka testlerin verisi karışmasın diye yazar id'si benzersiz.
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaAuditingConfig.class)
class CommentVisibleByAuthorRepositoryTest {

    @Autowired private TestEntityManager em;
    @Autowired private CommentRepository commentRepository;

    private final String token = "cv" + System.nanoTime();
    private final Long author = 7_000_000_000L + System.nanoTime() % 1_000_000;
    private final Long otherAuthor = author + 1;

    private Portal portal(String suffix, PortalStatus status) {
        return em.persist(Portal.builder().slug(token + "-" + suffix).nameTr(token + suffix).nameEn(token + suffix)
                .status(status).sortOrder(0).memberCount(0).build());
    }

    private Thread thread(Portal portal, ThreadStatus status) {
        return em.persist(Thread.builder().slug("t-" + System.nanoTime()).surface(ThreadSurface.THEORY)
                .title("başlık").status(status).authorId(1L).portalId(portal.getId()).build());
    }

    private Comment comment(Long authorId, CommentSubjectType type, Long subjectId, CommentStatus status) {
        return em.persist(Comment.builder().subjectType(type).subjectId(subjectId).body("yorum")
                .status(status).authorId(authorId).build());
    }

    private Page<Comment> visible(List<Long> hiddenPortalIds) {
        em.flush();
        em.clear();
        return commentRepository.findVisibleByAuthor(author, CommentStatus.PUBLISHED, CommentSubjectType.THREAD,
                ThreadStatus.PUBLISHED, hiddenPortalIds, PageRequest.of(0, 20));
    }

    @Test
    void publishedThreadComment_isVisible_deletedThreadAndDeletedCommentAreNot() {
        Portal open = portal("open", PortalStatus.ACTIVE);
        Thread live = thread(open, ThreadStatus.PUBLISHED);
        Thread gone = thread(open, ThreadStatus.DELETED);
        Comment shown = comment(author, CommentSubjectType.THREAD, live.getId(), CommentStatus.PUBLISHED);
        comment(author, CommentSubjectType.THREAD, gone.getId(), CommentStatus.PUBLISHED);
        comment(author, CommentSubjectType.THREAD, live.getId(), CommentStatus.DELETED);

        Page<Comment> result = visible(List.of(-1L));

        assertThat(result.getContent()).extracting(Comment::getId).containsExactly(shown.getId());
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    void commentOnThreadInHiddenPortal_isExcluded_archivedPortalStaysVisible() {
        Portal hidden = portal("hidden", PortalStatus.HIDDEN);
        Portal archived = portal("archived", PortalStatus.ARCHIVED);
        Thread inHidden = thread(hidden, ThreadStatus.PUBLISHED);
        Thread inArchived = thread(archived, ThreadStatus.PUBLISHED);
        comment(author, CommentSubjectType.THREAD, inHidden.getId(), CommentStatus.PUBLISHED);
        Comment shown = comment(author, CommentSubjectType.THREAD, inArchived.getId(), CommentStatus.PUBLISHED);

        Page<Comment> result = visible(List.of(hidden.getId()));

        assertThat(result.getContent()).extracting(Comment::getId).containsExactly(shown.getId());
    }

    @Test
    void nonThreadComments_areIncludedWithoutThreadLookup_andOtherAuthorsAreNot() {
        Comment onBlog = comment(author, CommentSubjectType.BLOG, 123456L, CommentStatus.PUBLISHED);
        Comment onEpisode = comment(author, CommentSubjectType.EPISODE, 654321L, CommentStatus.PUBLISHED);
        comment(otherAuthor, CommentSubjectType.BLOG, 123456L, CommentStatus.PUBLISHED);

        Page<Comment> result = visible(List.of(-1L));

        assertThat(result.getContent()).extracting(Comment::getId)
                .containsExactlyInAnyOrder(onBlog.getId(), onEpisode.getId());
    }

    @Test
    void ordersNewestFirst() {
        Comment older = comment(author, CommentSubjectType.BLOG, 1L, CommentStatus.PUBLISHED);
        Comment newer = comment(author, CommentSubjectType.BLOG, 2L, CommentStatus.PUBLISHED);
        em.flush();
        // created_at @CreatedDate ile persist anında ezilir (updatable=false): yaş native UPDATE ile verilir.
        em.getEntityManager().createNativeQuery("UPDATE comment SET created_at = :ts WHERE id = :id")
                .setParameter("ts", LocalDateTime.now().minusDays(3)).setParameter("id", older.getId())
                .executeUpdate();

        Page<Comment> result = visible(List.of(-1L));

        assertThat(result.getContent()).extracting(Comment::getId).containsExactly(newer.getId(), older.getId());
    }
}
