package com.example.fandoom_backend.community.repository;

import com.example.fandoom_backend.community.entity.ThreadTag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface ThreadTagRepository extends JpaRepository<ThreadTag, Long> {

    List<ThreadTag> findByThread_Id(Long threadId);

    List<ThreadTag> findByThread_IdIn(Collection<Long> threadIds);

    // GET /threads?tag=x filtresi için — thread.id'ye map edilir (ThreadServiceImpl).
    List<ThreadTag> findByTag(String tag);

    void deleteByThread_Id(Long threadId);
}
