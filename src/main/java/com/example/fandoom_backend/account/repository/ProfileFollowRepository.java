package com.example.fandoom_backend.account.repository;

import com.example.fandoom_backend.account.entity.FollowState;
import com.example.fandoom_backend.account.entity.ProfileFollow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface ProfileFollowRepository extends JpaRepository<ProfileFollow, Long> {

    Optional<ProfileFollow> findByFollowerIdAndFollowedId(Long followerId, Long followedId);

    List<ProfileFollow> findByFollowerIdAndFollowedIdIn(Long followerId, Collection<Long> followedIds);

    long countByFollowedIdAndStatus(Long followedId, FollowState status);

    long countByFollowerIdAndStatus(Long followerId, FollowState status);

    // En yeni takip önce. Sıralama sorguda sabit; Pageable'ın sort'u kullanılmaz.
    @Query("select f.followerId from ProfileFollow f where f.followedId = :userId and f.status = :status "
            + "order by f.createdAt desc, f.id desc")
    Page<Long> findFollowerIds(@Param("userId") Long userId, @Param("status") FollowState status, Pageable pageable);

    @Query("select f.followedId from ProfileFollow f where f.followerId = :userId and f.status = :status "
            + "order by f.createdAt desc, f.id desc")
    Page<Long> findFollowedIds(@Param("userId") Long userId, @Param("status") FollowState status, Pageable pageable);

    // Eşzamanlı çift istekte unique kısıt ihlali istisna fırlatmaz (istisna @Transactional'ı rollback-only
    // yapardı): 1 = satır oluştu, 0 = zaten vardı. Auditing atlandığı için zaman damgaları açıkça verilir.
    @Modifying
    @Query(value = "INSERT IGNORE INTO profile_follow (created_at, updated_at, follower_id, followed_id, status) "
            + "VALUES (:now, :now, :followerId, :followedId, :status)", nativeQuery = true)
    int insertIgnore(@Param("followerId") Long followerId, @Param("followedId") Long followedId,
                     @Param("status") String status, @Param("now") LocalDateTime now);

    // Gizlilik kapanınca bekleyen istekleri onaylamak için: her istek sahibine olay yayınlanacağından satırlar çekilir.
    List<ProfileFollow> findByFollowedIdAndStatus(Long followedId, FollowState status);

    // Liste satırlarındaki "following" bayrağı için sayfa başına tek sorgu (N+1 yok).
    @Query("select f.followedId from ProfileFollow f where f.followerId = :viewerId "
            + "and f.status = :status and f.followedId in :ids")
    Set<Long> findFollowedIdsAmong(@Param("viewerId") Long viewerId, @Param("status") FollowState status,
                                   @Param("ids") Collection<Long> ids);
}
