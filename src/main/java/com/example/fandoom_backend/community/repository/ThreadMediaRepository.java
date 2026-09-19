package com.example.fandoom_backend.community.repository;

import com.example.fandoom_backend.community.entity.ThreadMedia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface ThreadMediaRepository extends JpaRepository<ThreadMedia, Long> {

    List<ThreadMedia> findByThread_IdOrderByPositionAscIdAsc(Long threadId);

    // Feed/liste sayfası için toplu yükleme (N+1 yok): tek IN sorgusu.
    List<ThreadMedia> findByThread_IdInOrderByPositionAscIdAsc(Collection<Long> threadIds);

    boolean existsByUrl(String url);
}
