package com.example.fandoom_backend.community.repository;

import com.example.fandoom_backend.community.entity.ThreadBookmark;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ThreadBookmarkRepository extends JpaRepository<ThreadBookmark, Long> {

    boolean existsByUserIdAndThreadId(Long userId, Long threadId);

    void deleteByUserIdAndThreadId(Long userId, Long threadId);
}
