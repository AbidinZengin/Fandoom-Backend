package com.example.fandoom_backend.community.repository;

import com.example.fandoom_backend.community.entity.Thread;
import com.example.fandoom_backend.community.entity.ThreadStatus;
import com.example.fandoom_backend.community.entity.ThreadSurface;
import org.springframework.data.domain.Pageable;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
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
    // TIMESTAMPDIFF(HOUR, ...) tam saatleri sayar (küsurat atılır). GREATEST(..., 0): geleceğe tarihli created_at
    // (saat kayması, farklı saat dilimi, hatalı veri) negatif yaş verir; yaş -2 olunca payda POW(0, 1.5) = 0 olur ve
    // MySQL strict modda 'Division by 0' ile TÜM pencereyi düşürürdü. Yaş 0'a kırpılır (en yeni gibi sayılır). MySQL'e özgü native SQL (projedeki diğer
    // native sorgular gibi). :now parametre olarak verilir (SQL NOW() DB oturum saat dilimini kullanır;
    // created_at ise JVM saat diliminde yazılmıştır). MySQL, değeri değişmeyen satırı fiilen yazmaz.
    // Yalnızca hot_score kolonu yazılır: like_count/comment_count'a ve updated_at'e dokunulmaz.
    @Modifying
    @Query(value = "UPDATE thread SET hot_score = "
            + "(like_count + 2.0 * comment_count) / POW(GREATEST(TIMESTAMPDIFF(HOUR, created_at, :now), 0) + 2, 1.5) "
            + "WHERE status = :status AND id >= :fromId AND id < :toId", nativeQuery = true)
    int recalculateHotScores(@Param("status") String status, @Param("fromId") long fromId,
                             @Param("toId") long toId, @Param("now") LocalDateTime now);

    // Public okuma yolu: sadece PUBLISHED VE portalı HIDDEN olmayan thread (HIDDEN portal/içindeki thread sızdırılmaz -> 404).
    // Tek sorgu (correlated NOT EXISTS, portal PK araması) — ayrı bir portal okuması gerekmez.
    @Query("SELECT t FROM Thread t WHERE t.slug = :slug AND t.status = com.example.fandoom_backend.community.entity.ThreadStatus.PUBLISHED "
            + "AND NOT EXISTS (SELECT 1 FROM Portal p WHERE p.id = t.portalId "
            + "AND p.status = com.example.fandoom_backend.community.entity.PortalStatus.HIDDEN)")
    Optional<Thread> findVisibleBySlug(@Param("slug") String slug);

    @Query("SELECT t FROM Thread t WHERE t.id = :id AND t.status = com.example.fandoom_backend.community.entity.ThreadStatus.PUBLISHED "
            + "AND NOT EXISTS (SELECT 1 FROM Portal p WHERE p.id = t.portalId "
            + "AND p.status = com.example.fandoom_backend.community.entity.PortalStatus.HIDDEN)")
    Optional<Thread> findVisibleById(@Param("id") Long id);

    // Yorum listeleme uçları thread durumundan bağımsız çalışır (DELETED thread'in yorumları okunabilir — mevcut davranış);
    // ama HIDDEN portaldaki thread'in yorumları sızdırılmamalı.
    @Query("SELECT COUNT(t) > 0 FROM Thread t WHERE t.id = :id AND EXISTS (SELECT 1 FROM Portal p WHERE p.id = t.portalId "
            + "AND p.status = com.example.fandoom_backend.community.entity.PortalStatus.HIDDEN)")
    boolean isInHiddenPortal(@Param("id") Long id);

    // ARCHIVED portal "yazma kapalı": thread PATCH / yorum ekleme / like / bookmark bu sorguyla reddedilir (DELETE ve
    // unlike/unbookmark serbest).
    @Query("SELECT COUNT(t) > 0 FROM Thread t WHERE t.id = :id AND EXISTS (SELECT 1 FROM Portal p WHERE p.id = t.portalId "
            + "AND p.status = com.example.fandoom_backend.community.entity.PortalStatus.ARCHIVED)")
    boolean isInArchivedPortal(@Param("id") Long id);

    boolean existsBySlug(String slug);

    // Silme: "PUBLISHED ise DELETED yap" TEK atomik UPDATE. Etkilenen satır 1 ise (yalnız o çağrı thread'i gerçekten
    // PUBLISHED'dan çıkardı) portal sayacı düşürülür -> eşzamanlı çift DELETE sayacı iki kez düşüremez.
    // updated_at bulk UPDATE'te auditing'den geçmediği için elle set edilir.
    @Modifying
    @Query("UPDATE Thread t SET t.status = com.example.fandoom_backend.community.entity.ThreadStatus.DELETED, t.updatedAt = :now "
            + "WHERE t.id = :id AND t.status = com.example.fandoom_backend.community.entity.ThreadStatus.PUBLISHED")
    int markDeletedIfPublished(@Param("id") Long id, @Param("now") LocalDateTime now);

    // Sayaca dahil olmayan (HIDDEN) thread'i de silinebilir bırakır (eski davranış: her durumdan DELETED), sayaca dokunmaz.
    @Modifying
    @Query("UPDATE Thread t SET t.status = com.example.fandoom_backend.community.entity.ThreadStatus.DELETED, t.updatedAt = :now "
            + "WHERE t.id = :id AND t.status = com.example.fandoom_backend.community.entity.ThreadStatus.HIDDEN")
    int markDeletedIfHidden(@Param("id") Long id, @Param("now") LocalDateTime now);

    // Portal taşıma: thread satırı SELECT ... FOR UPDATE ile kilitlenir (eski portal_id'yi okuyup yanlış sayaç
    // düşürme / kayıp güncelleme riskine karşı; okuma "güncel okuma"dır).
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Thread> findWithLockById(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Thread> findWithLockBySlug(String slug);

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
