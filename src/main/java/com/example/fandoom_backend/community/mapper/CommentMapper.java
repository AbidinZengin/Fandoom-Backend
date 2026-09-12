package com.example.fandoom_backend.community.mapper;

import com.example.fandoom_backend.community.dto.AuthorSummary;
import com.example.fandoom_backend.community.dto.CommentResponse;
import com.example.fandoom_backend.community.entity.Comment;
import com.example.fandoom_backend.community.entity.CommentStatus;
import com.example.fandoom_backend.community.entity.CommentSubjectType;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

// replyCount/replies/author/isLiked entity'nin alanı değil, servis katmanında
// hesaplanıp/çözülüp ekstra parametrelerle geçirilir. body: status==DELETED
// ise "[silindi]" olarak maskelenir, orijinal veri DB'de korunur (moderasyon izni).
// threadId: geriye uyumluluk için subjectType/subjectId'den türetilir.
@Mapper(componentModel = "spring")
public interface CommentMapper {

    @Mapping(target = "id", source = "comment.id")
    @Mapping(target = "body", expression = "java(maskIfDeleted(comment))")
    @Mapping(target = "subjectType", source = "comment.subjectType")
    @Mapping(target = "subjectId", source = "comment.subjectId")
    @Mapping(target = "threadId", expression = "java(threadIdOf(comment))")
    @Mapping(target = "parentId", source = "comment.parent.id")
    @Mapping(target = "replyCount", source = "replyCount")
    @Mapping(target = "replies", source = "replies")
    @Mapping(target = "author", source = "author")
    @Mapping(target = "isLiked", source = "liked")
    CommentResponse toResponse(
            Comment comment, int replyCount, List<CommentResponse> replies, AuthorSummary author, boolean liked);

    default String maskIfDeleted(Comment comment) {
        return comment.getStatus() == CommentStatus.DELETED ? "[silindi]" : comment.getBody();
    }

    default Long threadIdOf(Comment comment) {
        return comment.getSubjectType() == CommentSubjectType.THREAD ? comment.getSubjectId() : null;
    }
}
