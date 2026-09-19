package com.example.fandoom_backend.community.specification;

import com.example.fandoom_backend.community.entity.Comment;
import com.example.fandoom_backend.community.entity.CommentSubjectType;
import org.springframework.data.jpa.domain.Specification;

// Bir konunun (thread/blog/sezon/bölüm) üst seviye yorumları — DELETED olanlar dahil,
// reply zinciri/sayfalama yapısı kırılmasın diye (body mapper'da maskelenir).
public final class CommentSpecificationBuilder {

    private CommentSpecificationBuilder() {
    }

    public static Specification<Comment> topLevelOf(CommentSubjectType subjectType, Long subjectId) {
        return (root, query, cb) -> cb.and(
                cb.equal(root.get("subjectType"), subjectType),
                cb.equal(root.get("subjectId"), subjectId),
                cb.isNull(root.get("parent")));
    }
}
