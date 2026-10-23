package com.example.fandoom_backend.account.repository;

import com.example.fandoom_backend.account.entity.FollowState;
import com.example.fandoom_backend.account.entity.ProfileFollow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

public interface ProfileFollowRepository extends JpaRepository<ProfileFollow, Long> {

    Optional<ProfileFollow> findByFollowerIdAndFollowedId(Long followerId, Long followedId);

    long countByFollowedIdAndStatus(Long followedId, FollowState status);

    long countByFollowerIdAndStatus(Long followerId, FollowState status);

    // En yeni takip önce. Sıralama sorguda sabit; Pageable'ın sort'u kullanılmaz.
    @Query("select f.followerId from ProfileFollow f where f.followedId = :userId and f.status = :status "
            + "order by f.createdAt desc, f.id desc")
    Page<Long> findFollowerIds(@Param("userId") Long userId, @Param("status") FollowState status, Pageable pageable);

    @Query("select f.followedId from ProfileFollow f where f.followerId = :userId and f.status = :status "
            + "order by f.createdAt desc, f.id desc")
    Page<Long> findFollowedIds(@Param("userId") Long userId, @Param("status") FollowState status, Pageable pageable);

    // Liste satırlarındaki "following" bayrağı için sayfa başına tek sorgu (N+1 yok).
    @Query("select f.followedId from ProfileFollow f where f.followerId = :viewerId "
            + "and f.status = :status and f.followedId in :ids")
    Set<Long> findFollowedIdsAmong(@Param("viewerId") Long viewerId, @Param("status") FollowState status,
                                   @Param("ids") Collection<Long> ids);
}
