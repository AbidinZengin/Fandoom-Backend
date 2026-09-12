package com.example.fandoom_backend.community.repository;

import com.example.fandoom_backend.community.entity.Comment;
import com.example.fandoom_backend.community.entity.CommentStatus;
import com.example.fandoom_backend.community.entity.CommentSubjectType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

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

    @Modifying
    @Query("UPDATE Comment c SET c.likeCount = c.likeCount + 1 WHERE c.id = :id")
    void incrementLikeCount(@Param("id") Long id);

    @Modifying
    @Query("UPDATE Comment c SET c.likeCount = c.likeCount - 1 WHERE c.id = :id")
    void decrementLikeCount(@Param("id") Long id);
}
