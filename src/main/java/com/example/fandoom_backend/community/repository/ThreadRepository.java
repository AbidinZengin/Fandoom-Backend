package com.example.fandoom_backend.community.repository;

import com.example.fandoom_backend.community.entity.Thread;
import com.example.fandoom_backend.community.entity.ThreadStatus;
import com.example.fandoom_backend.community.entity.ThreadSurface;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ThreadRepository extends JpaRepository<Thread, Long>, JpaSpecificationExecutor<Thread> {

    // Herhangi bir status'ta arar — sahip/moderatör update/delete akışı için
    // (silinmiş bir thread'i tekrar silmek idempotent kalsın diye).
    Optional<Thread> findBySlug(String slug);

    // HotScoreJob için PK aralığı sınırları: ORDER BY id LIMIT 1 (satır yüklemez, sabit maliyet).
    @Query("SELECT t.id FROM Thread t WHERE t.status = :status ORDER BY t.id ASC")
    List<Long> findIdsByStatusAscending(@Param("status") ThreadStatus status, Pageable pageable);

    @Query("SELECT t.id FROM Thread t WHERE t.status = :status ORDER BY t.id DESC")
    List<Long> findIdsByStatusDescending(@Param("status") ThreadStatus status, Pageable pageable);

    // "hot" skorunun TEK kaynağı: hesap ve yazma veritabanında, [fromId, toId) aralığı için tek statement.
    //   skor = (like_count + 2*comment_count) / (yaşSaat + 2)^1.5
    // TIMESTAMPDIFF(HOUR, ...) tam saatleri sayar (küsurat atılır). MySQL'e özgü native SQL (projedeki diğer
    // native sorgular gibi). :now parametre olarak verilir (SQL NOW() DB oturum saat dilimini kullanır;
    // created_at ise JVM saat diliminde yazılmıştır). MySQL, değeri değişmeyen satırı fiilen yazmaz.
    // Yalnızca hot_score kolonu yazılır: like_count/comment_count'a ve updated_at'e dokunulmaz.
    @Modifying
    @Query(value = "UPDATE thread SET hot_score = "
            + "(like_count + 2.0 * comment_count) / POW(TIMESTAMPDIFF(HOUR, created_at, :now) + 2, 1.5) "
            + "WHERE status = :status AND id >= :fromId AND id < :toId", nativeQuery = true)
    int recalculateHotScores(@Param("status") String status, @Param("fromId") long fromId,
                             @Param("toId") long toId, @Param("now") LocalDateTime now);

    // Public okuma yolu (getBySlug) sadece PUBLISHED döner.
    Optional<Thread> findBySlugAndStatus(String slug, ThreadStatus status);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, Long id);

    long countByAuthorIdAndSurfaceAndStatus(Long authorId, ThreadSurface surface, ThreadStatus status);

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
