package com.example.fandoom_backend.community.repository;

import com.example.fandoom_backend.community.entity.Portal;
import com.example.fandoom_backend.community.entity.PortalStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PortalRepository extends JpaRepository<Portal, Long> {

    Optional<Portal> findBySlug(String slug);

    boolean existsBySlug(String slug);

    // Join/leave: SELECT ... FOR UPDATE. Portal satırı transaction boyunca kilitli -> aynı portala eşzamanlı join/leave
    // sıralanır (kilit sırası her zaman portal -> membership: FK'nın portal satırına aldığı S-kilidin X'e yükseltilmesinden
    // doğacak deadlock oluşmaz) ve okunan member_count "güncel okuma"dır (bayat snapshot değil).
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Portal> findWithLockBySlug(String slug);

    // Public okumalardan gizlenecek portal id'leri (genelde boş/çok küçük küme).
    @Query("SELECT p.id FROM Portal p WHERE p.status = :status")
    List<Long> findIdsByStatus(@Param("status") PortalStatus status);

    // Sayaçlar: entity'nin in-memory alanı bilerek set edilmez (Thread sayaçlarıyla aynı desen — bulk UPDATE'i
    // stale dirty-checking ezmesin). Decrement 0'ın altına inmez (CHECK member_count/thread_count >= 0).
    @Modifying
    @Query("UPDATE Portal p SET p.memberCount = p.memberCount + 1 WHERE p.id = :id")
    void incrementMemberCount(@Param("id") Long id);

    @Modifying
    @Query("UPDATE Portal p SET p.memberCount = p.memberCount - 1 WHERE p.id = :id AND p.memberCount > 0")
    void decrementMemberCount(@Param("id") Long id);

    @Modifying
    @Query("UPDATE Portal p SET p.threadCount = p.threadCount + 1 WHERE p.id = :id")
    void incrementThreadCount(@Param("id") Long id);

    @Modifying
    @Query("UPDATE Portal p SET p.threadCount = p.threadCount - 1 WHERE p.id = :id AND p.threadCount > 0")
    void decrementThreadCount(@Param("id") Long id);

    // Public dizin: verilen statüler (ACTIVE+ARCHIVED) + ad araması (TR+EN). :pattern hazır LIKE kalıbıdır
    // (caller wildcard'ları '!' ile escape eder; ESCAPE '!' seçildi çünkü '\' MySQL string literal'inde kaçış karakteri).
    @Query("SELECT p FROM Portal p WHERE p.status IN :statuses "
            + "AND (LOWER(p.nameTr) LIKE :pattern ESCAPE '!' OR LOWER(p.nameEn) LIKE :pattern ESCAPE '!')")
    Page<Portal> search(@Param("statuses") Collection<PortalStatus> statuses, @Param("pattern") String pattern,
                        Pageable pageable);

    // Trending için sayfalamasız aynı küme (portal tablosu küçük; skor bellekte sıralanır).
    @Query("SELECT p FROM Portal p WHERE p.status IN :statuses "
            + "AND (LOWER(p.nameTr) LIKE :pattern ESCAPE '!' OR LOWER(p.nameEn) LIKE :pattern ESCAPE '!')")
    List<Portal> searchAll(@Param("statuses") Collection<PortalStatus> statuses, @Param("pattern") String pattern);

    // Trending girdileri (formül PortalQueryServiceImpl'de TEK yerde): portal başına 'since' sonrası yeni PUBLISHED
    // thread sayısı ve o portalın PUBLISHED thread'lerine yazılmış PUBLISHED yorum sayısı. [portalId, count] satırları.
    @Query("SELECT t.portalId, COUNT(t) FROM Thread t WHERE t.status = com.example.fandoom_backend.community.entity.ThreadStatus.PUBLISHED "
            + "AND t.createdAt >= :since AND t.portalId IN :portalIds GROUP BY t.portalId")
    List<Object[]> countNewThreadsSince(@Param("portalIds") Collection<Long> portalIds, @Param("since") LocalDateTime since);

    @Query("SELECT t.portalId, COUNT(c) FROM Comment c JOIN Thread t ON t.id = c.subjectId "
            + "WHERE c.subjectType = com.example.fandoom_backend.community.entity.CommentSubjectType.THREAD "
            + "AND c.status = com.example.fandoom_backend.community.entity.CommentStatus.PUBLISHED "
            + "AND c.createdAt >= :since "
            + "AND t.status = com.example.fandoom_backend.community.entity.ThreadStatus.PUBLISHED "
            + "AND t.portalId IN :portalIds GROUP BY t.portalId")
    List<Object[]> countNewCommentsSince(@Param("portalIds") Collection<Long> portalIds, @Param("since") LocalDateTime since);
}
