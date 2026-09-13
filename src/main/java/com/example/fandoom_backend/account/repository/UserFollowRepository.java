package com.example.fandoom_backend.account.repository;

import com.example.fandoom_backend.account.entity.SavedItemType;
import com.example.fandoom_backend.account.entity.UserFollow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserFollowRepository extends JpaRepository<UserFollow, Long> {
    Optional<UserFollow> findByUserIdAndItemTypeAndItemId(Long userId, SavedItemType itemType, Long itemId);
    boolean existsByUserIdAndItemTypeAndItemId(Long userId, SavedItemType itemType, Long itemId);
    long countByItemTypeAndItemId(SavedItemType itemType, Long itemId);
    // Ters yön: "BU kullanıcı kaç şeyi takip ediyor" (countByItemTypeAndItemId
    // "BUNU kaç kişi takip ediyor" sorusunun tersi).
    long countByUserIdAndItemType(Long userId, SavedItemType itemType);
    Page<UserFollow> findByUserId(Long userId, Pageable pageable);
}
