package com.example.fandoom_backend.blog.repository;

import com.example.fandoom_backend.blog.entity.BlogRelation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BlogRelationRepository extends JpaRepository<BlogRelation, Long> {
    List<BlogRelation> findBySourceBlogIdOrderByOrderIndexAsc(Long sourceBlogId);
}
