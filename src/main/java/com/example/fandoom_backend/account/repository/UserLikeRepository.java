package com.example.fandoom_backend.account.repository;

import com.example.fandoom_backend.account.entity.SavedItemType;
import com.example.fandoom_backend.account.entity.UserLike;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserLikeRepository extends JpaRepository<UserLike, Long> {
    Optional<UserLike> findByUserIdAndItemTypeAndItemId(Long userId, SavedItemType itemType, Long itemId);
    boolean existsByUserIdAndItemTypeAndItemId(Long userId, SavedItemType itemType, Long itemId);
    long countByItemTypeAndItemId(SavedItemType itemType, Long itemId);
    long countByUserId(Long userId);
    Page<UserLike> findByUserId(Long userId, Pageable pageable);
}
