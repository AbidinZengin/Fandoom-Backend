package com.example.fandoom_backend.community.repository;

import com.example.fandoom_backend.community.entity.ThreadLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Set;

public interface ThreadLikeRepository extends JpaRepository<ThreadLike, Long> {

    boolean existsByUserIdAndThreadId(Long userId, Long threadId);

    void deleteByUserIdAndThreadId(Long userId, Long threadId);

    // isLiked zenginleştirmesi için toplu kontrol — bir sayfadaki tüm thread
    // id'leri tek sorguda kontrol edilir, N+1 önlenir.
    @Query("SELECT tl.threadId FROM ThreadLike tl WHERE tl.userId = :userId AND tl.threadId IN :threadIds")
    Set<Long> findThreadIdsByUserIdAndThreadIdIn(@Param("userId") Long userId, @Param("threadIds") Collection<Long> threadIds);
}
