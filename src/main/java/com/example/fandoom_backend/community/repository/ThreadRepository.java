package com.example.fandoom_backend.community.repository;

import com.example.fandoom_backend.community.entity.Thread;
import com.example.fandoom_backend.community.entity.ThreadStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ThreadRepository extends JpaRepository<Thread, Long>, JpaSpecificationExecutor<Thread> {

    // Herhangi bir status'ta arar — sahip/moderatör update/delete akışı için
    // (silinmiş bir thread'i tekrar silmek idempotent kalsın diye).
    Optional<Thread> findBySlug(String slug);

    // HotScoreJob için — Faz 1'de düz liste yeterli, sayfalama yok (tüm
    // PUBLISHED thread'ler her çalıştırmada yeniden hesaplanır).
    List<Thread> findByStatus(ThreadStatus status);

    // Public okuma yolu (getBySlug) sadece PUBLISHED döner.
    Optional<Thread> findBySlugAndStatus(String slug, ThreadStatus status);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, Long id);

    @Modifying
    @Query("UPDATE Thread t SET t.likeCount = t.likeCount + 1 WHERE t.id = :id")
    void incrementLikeCount(@Param("id") Long id);

    @Modifying
    @Query("UPDATE Thread t SET t.likeCount = t.likeCount - 1 WHERE t.id = :id")
    void decrementLikeCount(@Param("id") Long id);

    @Modifying
    @Query("UPDATE Thread t SET t.bookmarkCount = t.bookmarkCount + 1 WHERE t.id = :id")
    void incrementBookmarkCount(@Param("id") Long id);

    @Modifying
    @Query("UPDATE Thread t SET t.bookmarkCount = t.bookmarkCount - 1 WHERE t.id = :id")
    void decrementBookmarkCount(@Param("id") Long id);

    @Modifying
    @Query("UPDATE Thread t SET t.commentCount = t.commentCount + 1 WHERE t.id = :id")
    void incrementCommentCount(@Param("id") Long id);

    @Modifying
    @Query("UPDATE Thread t SET t.commentCount = t.commentCount - 1 WHERE t.id = :id")
    void decrementCommentCount(@Param("id") Long id);
}
