package com.example.fandoom_backend.account.repository;

import com.example.fandoom_backend.account.entity.UserSavedItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserSavedItemRepository extends JpaRepository<UserSavedItem, Long> {
    Page<UserSavedItem> findByUserListId(Long userListId, Pageable pageable);
    long countByUserListId(Long userListId);
    Optional<UserSavedItem> findByIdAndUserId(Long id, Long userId);
    void deleteByUserListId(Long userListId);
}
