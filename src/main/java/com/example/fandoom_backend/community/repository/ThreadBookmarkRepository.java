package com.example.fandoom_backend.community.repository;

import com.example.fandoom_backend.community.entity.ThreadBookmark;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Set;

public interface ThreadBookmarkRepository extends JpaRepository<ThreadBookmark, Long> {

    boolean existsByUserIdAndThreadId(Long userId, Long threadId);

    void deleteByUserIdAndThreadId(Long userId, Long threadId);

    // isBookmarked zenginleştirmesi için toplu kontrol.
    @Query("SELECT tb.threadId FROM ThreadBookmark tb WHERE tb.userId = :userId AND tb.threadId IN :threadIds")
    Set<Long> findThreadIdsByUserIdAndThreadIdIn(@Param("userId") Long userId, @Param("threadIds") Collection<Long> threadIds);
}
