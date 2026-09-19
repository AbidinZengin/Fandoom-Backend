package com.example.fandoom_backend.community.repository;

import com.example.fandoom_backend.community.entity.Comment;
import com.example.fandoom_backend.community.entity.CommentStatus;
import com.example.fandoom_backend.community.entity.CommentSubjectType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long>, JpaSpecificationExecutor<Comment> {

    long countByAuthorIdAndStatus(Long authorId, CommentStatus status);

    // Üst seviye yorumlar (parent IS NULL) — DELETED olanlar da dahil, yapı
    // (reply zinciri, sayfalama) kırılmasın diye; body mapper'da maskelenir.
    Page<Comment> findBySubjectTypeAndSubjectIdAndParentIsNullOrderByCreatedAtDesc(
            CommentSubjectType subjectType, Long subjectId, Pageable pageable);

    Page<Comment> findBySubjectTypeAndSubjectIdAndParentIsNullOrderByLikeCountDesc(
            CommentSubjectType subjectType, Long subjectId, Pageable pageable);

    // İlk 3 yanıt önizlemesi için: Pageable ile çağrılır (PageRequest.of(0, 3)).
    List<Comment> findByParent_IdOrderByCreatedAtAsc(Long parentId, Pageable pageable);

    long countByParent_Id(Long parentId);

    // Toplu yanıt önizlemesi (N+1 yerine sayfa başına 2 sorgu): her parent için ilk
    // :limit yanıt, MySQL 8 pencere fonksiyonuyla (PARTITION BY parent_id). Sonuç
    // parent_id, created_at, id sırasındadır (idx_comment_parent_created kapsar).
    @Query(value = "SELECT r.* FROM (SELECT c.*, ROW_NUMBER() OVER (PARTITION BY c.parent_id "
            + "ORDER BY c.created_at ASC, c.id ASC) AS rn FROM comment c WHERE c.parent_id IN (:parentIds)) r "
            + "WHERE r.rn <= :limit ORDER BY r.parent_id, r.created_at, r.id", nativeQuery = true)
    List<Comment> findFirstRepliesByParentIds(
            @Param("parentIds") Collection<Long> parentIds, @Param("limit") int limit);

    // [parentId, replyCount] çiftleri — tek GROUP BY sorgusu.
    @Query("SELECT c.parent.id, COUNT(c) FROM Comment c WHERE c.parent.id IN :parentIds GROUP BY c.parent.id")
    List<Object[]> countRepliesByParentIds(@Param("parentIds") Collection<Long> parentIds);

    @Modifying
    @Query("UPDATE Comment c SET c.likeCount = c.likeCount + 1 WHERE c.id = :id")
    void incrementLikeCount(@Param("id") Long id);

    @Modifying
    @Query("UPDATE Comment c SET c.likeCount = c.likeCount - 1 WHERE c.id = :id")
    void decrementLikeCount(@Param("id") Long id);
}
