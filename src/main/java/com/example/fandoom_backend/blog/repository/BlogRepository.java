package com.example.fandoom_backend.blog.repository;

import com.example.fandoom_backend.blog.entity.Blog;
import com.example.fandoom_backend.blog.entity.BlogStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface BlogRepository extends JpaRepository<Blog, Long>, JpaSpecificationExecutor<Blog> {

    Optional<Blog> findBySlug(String slug);

    Optional<Blog> findBySlugAndStatus(String slug, BlogStatus status);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, Long id);

    @Modifying
    @Query("UPDATE Blog b SET b.viewCount = b.viewCount + 1 WHERE b.id = :id")
    void incrementViewCount(@Param("id") Long id);

    // "Recency+popülerlik" fallback kademesi — merdivenin son basamağı.
    List<Blog> findByStatusOrderByPublishedAtDescViewCountDesc(BlogStatus status, Pageable pageable);

    List<Blog> findByStatusAndIdNotInOrderByPublishedAtDescViewCountDesc(
            BlogStatus status, Collection<Long> excludedIds, Pageable pageable);
}
