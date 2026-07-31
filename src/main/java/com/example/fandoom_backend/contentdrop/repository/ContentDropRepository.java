package com.example.fandoom_backend.contentdrop.repository;

import com.example.fandoom_backend.contentdrop.entity.ContentDrop;
import com.example.fandoom_backend.contentdrop.entity.ContentDropStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ContentDropRepository extends JpaRepository<ContentDrop, Long> {

    Optional<ContentDrop> findBySlug(String slug);

    Optional<ContentDrop> findBySlugAndStatus(String slug, ContentDropStatus status);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, Long id);

    @Modifying
    @Query("UPDATE ContentDrop c SET c.viewCount = c.viewCount + 1 WHERE c.id = :id")
    void incrementViewCount(@Param("id") Long id);

    // "Recency+popülerlik" fallback kademesi — merdivenin son basamağı.
    List<ContentDrop> findByStatusOrderByPublishedAtDescViewCountDesc(ContentDropStatus status, Pageable pageable);

    List<ContentDrop> findByStatusAndIdNotInOrderByPublishedAtDescViewCountDesc(
            ContentDropStatus status, Collection<Long> excludedIds, Pageable pageable);
}
