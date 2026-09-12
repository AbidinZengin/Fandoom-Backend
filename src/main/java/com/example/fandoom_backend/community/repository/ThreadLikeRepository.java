package com.example.fandoom_backend.community.repository;

import com.example.fandoom_backend.community.entity.ThreadLike;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ThreadLikeRepository extends JpaRepository<ThreadLike, Long> {

    boolean existsByUserIdAndThreadId(Long userId, Long threadId);

    void deleteByUserIdAndThreadId(Long userId, Long threadId);
}
