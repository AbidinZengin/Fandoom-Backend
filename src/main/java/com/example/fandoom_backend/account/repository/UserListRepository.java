package com.example.fandoom_backend.account.repository;

import com.example.fandoom_backend.account.entity.ListType;
import com.example.fandoom_backend.account.entity.UserList;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserListRepository extends JpaRepository<UserList, Long> {
    List<UserList> findByUserIdOrderByIdAsc(Long userId);
    Optional<UserList> findByUserIdAndListType(Long userId, ListType listType);
    Optional<UserList> findByIdAndUserId(Long id, Long userId);
    long countByUserIdAndIsPinnedTrue(Long userId);
    List<UserList> findByUserIdAndIsPublicTrueAndIsPinnedTrueOrderByIdAsc(Long userId);
}
