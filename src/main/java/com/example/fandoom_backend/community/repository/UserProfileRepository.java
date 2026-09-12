package com.example.fandoom_backend.community.repository;

import com.example.fandoom_backend.community.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {
    Optional<UserProfile> findByUserId(Long userId);

    // avatarUrl toplu zenginleştirmesi için (username çözümüyle aynı toplu
    // sorgu deseni, bkz. UserService.getUsernamesByIds) — N+1 önler.
    List<UserProfile> findByUserIdIn(Collection<Long> userIds);
}
