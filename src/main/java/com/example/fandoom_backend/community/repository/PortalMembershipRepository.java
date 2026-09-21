package com.example.fandoom_backend.community.repository;

import com.example.fandoom_backend.community.entity.PortalMembership;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface PortalMembershipRepository extends JpaRepository<PortalMembership, Long> {

    boolean existsByPortalIdAndUserId(Long portalId, Long userId);

    // Sayfa başına tek sorgu: kullanıcının bu portallardan hangilerine üye olduğu (isMember, N+1 yok).
    @Query("SELECT m.portalId FROM PortalMembership m WHERE m.userId = :userId AND m.portalId IN :portalIds")
    Set<Long> findPortalIdsByUserIdAndPortalIdIn(@Param("userId") Long userId, @Param("portalIds") Collection<Long> portalIds);

    // "Portallarım": joinedAt azalan (idx_portal_membership_user), eşitlikte portal id.
    List<PortalMembership> findByUserIdOrderByJoinedAtDescPortalIdAsc(Long userId);

    // scope=joined: üye olunan ve HIDDEN olmayan (ARCHIVED dahil) portal id'leri.
    @Query("SELECT m.portalId FROM PortalMembership m JOIN Portal p ON p.id = m.portalId "
            + "WHERE m.userId = :userId AND p.status <> com.example.fandoom_backend.community.entity.PortalStatus.HIDDEN")
    Set<Long> findVisiblePortalIdsByUserId(@Param("userId") Long userId);

    // INSERT IGNORE: (portal_id,user_id) unique çakışmasında istisna fırlatmaz (transaction rollback-only olmaz),
    // 1 = gerçekten yeni satır, 0 = zaten üye. Sayaç yalnız 1 dönünce artırılır.
    @Modifying
    @Query(value = "INSERT IGNORE INTO portal_membership (portal_id, user_id, joined_at) "
            + "VALUES (:portalId, :userId, :joinedAt)", nativeQuery = true)
    int insertIgnore(@Param("portalId") Long portalId, @Param("userId") Long userId,
                     @Param("joinedAt") LocalDateTime joinedAt);

    // Silinen satır sayısı (0/1): sayaç yalnız 1 dönünce azaltılır.
    @Modifying
    @Query("DELETE FROM PortalMembership m WHERE m.portalId = :portalId AND m.userId = :userId")
    int deleteMembership(@Param("portalId") Long portalId, @Param("userId") Long userId);
}
