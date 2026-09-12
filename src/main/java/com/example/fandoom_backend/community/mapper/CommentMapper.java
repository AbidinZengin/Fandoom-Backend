package com.example.fandoom_backend.community.mapper;

import com.example.fandoom_backend.community.dto.CommentResponse;
import com.example.fandoom_backend.community.entity.Comment;
import com.example.fandoom_backend.community.entity.CommentStatus;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

// replyCount/replies entity'nin alanı değil, servis katmanında hesaplanıp
// ekstra parametrelerle geçirilir. body: status==DELETED ise "[silindi]"
// olarak maskelenir, orijinal veri DB'de korunur (moderasyon izni).
@Mapper(componentModel = "spring")
public interface CommentMapper {

    @Mapping(target = "body", expression = "java(maskIfDeleted(comment))")
    @Mapping(target = "threadId", source = "comment.thread.id")
    @Mapping(target = "parentId", source = "comment.parent.id")
    @Mapping(target = "replyCount", source = "replyCount")
    @Mapping(target = "replies", source = "replies")
    CommentResponse toResponse(Comment comment, int replyCount, List<CommentResponse> replies);

    default String maskIfDeleted(Comment comment) {
        return comment.getStatus() == CommentStatus.DELETED ? "[silindi]" : comment.getBody();
    }
}
