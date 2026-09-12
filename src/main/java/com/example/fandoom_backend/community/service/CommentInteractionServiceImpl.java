package com.example.fandoom_backend.community.service;

import com.example.fandoom_backend.common.exception.ResourceNotFoundException;
import com.example.fandoom_backend.community.dto.CommentLikeStatusResponse;
import com.example.fandoom_backend.community.entity.Comment;
import com.example.fandoom_backend.community.entity.CommentLike;
import com.example.fandoom_backend.community.repository.CommentLikeRepository;
import com.example.fandoom_backend.community.repository.CommentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentInteractionServiceImpl implements CommentInteractionService {

    private final CommentRepository commentRepository;
    private final CommentLikeRepository commentLikeRepository;

    @Override
    @Transactional
    public CommentLikeStatusResponse like(Long userId, Long commentId) {
        Comment comment = findComment(commentId);
        boolean alreadyLiked = commentLikeRepository.existsByUserIdAndCommentId(userId, commentId);
        if (!alreadyLiked) {
            commentLikeRepository.save(CommentLike.builder().userId(userId).commentId(commentId).build());
            commentRepository.incrementLikeCount(commentId);
        }
        int count = alreadyLiked ? comment.getLikeCount() : comment.getLikeCount() + 1;
        return new CommentLikeStatusResponse(true, count);
    }

    @Override
    @Transactional
    public CommentLikeStatusResponse unlike(Long userId, Long commentId) {
        Comment comment = findComment(commentId);
        boolean wasLiked = commentLikeRepository.existsByUserIdAndCommentId(userId, commentId);
        if (wasLiked) {
            commentLikeRepository.deleteByUserIdAndCommentId(userId, commentId);
            commentRepository.decrementLikeCount(commentId);
        }
        int count = wasLiked ? comment.getLikeCount() - 1 : comment.getLikeCount();
        return new CommentLikeStatusResponse(false, count);
    }

    private Comment findComment(Long id) {
        return commentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Yorum bulunamadı: id=" + id));
    }
}
