package com.example.fandoom_backend.account.repository;

import com.example.fandoom_backend.account.entity.SavedItemType;
import com.example.fandoom_backend.account.entity.UserBookmark;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserBookmarkRepository extends JpaRepository<UserBookmark, Long> {
    Optional<UserBookmark> findByUserIdAndItemTypeAndItemId(Long userId, SavedItemType itemType, Long itemId);
    boolean existsByUserIdAndItemTypeAndItemId(Long userId, SavedItemType itemType, Long itemId);
    long countByItemTypeAndItemId(SavedItemType itemType, Long itemId);
    Page<UserBookmark> findByUserId(Long userId, Pageable pageable);
}
