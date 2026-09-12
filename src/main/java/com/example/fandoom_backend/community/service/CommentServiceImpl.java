package com.example.fandoom_backend.community.service;

import com.example.fandoom_backend.common.dto.PageResponse;
import com.example.fandoom_backend.common.exception.InvalidReferenceException;
import com.example.fandoom_backend.common.exception.ResourceNotFoundException;
import com.example.fandoom_backend.community.dto.CommentRequest;
import com.example.fandoom_backend.community.dto.CommentResponse;
import com.example.fandoom_backend.community.entity.Comment;
import com.example.fandoom_backend.community.entity.CommentStatus;
import com.example.fandoom_backend.community.entity.Thread;
import com.example.fandoom_backend.community.entity.ThreadStatus;
import com.example.fandoom_backend.community.mapper.CommentMapper;
import com.example.fandoom_backend.community.repository.CommentRepository;
import com.example.fandoom_backend.community.repository.ThreadRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentServiceImpl implements CommentService {

    private static final int REPLIES_PREVIEW_SIZE = 3;

    private final CommentRepository commentRepository;
    private final ThreadRepository threadRepository;
    private final CommentMapper commentMapper;

    @Override
    public PageResponse<CommentResponse> listForThread(String threadSlug, String sort, Pageable pageable) {
        Thread thread = findPublishedThread(threadSlug);
        Page<Comment> page = "hot".equals(sort)
                ? commentRepository.findByThread_IdAndParentIsNullOrderByLikeCountDesc(thread.getId(), pageable)
                : commentRepository.findByThread_IdAndParentIsNullOrderByCreatedAtDesc(thread.getId(), pageable);
        return PageResponse.from(page.map(this::toResponseWithReplies));
    }

    @Override
    @Transactional
    public CommentResponse create(Long authorId, String threadSlug, CommentRequest request) {
        Thread thread = findPublishedThread(threadSlug);
        Comment parent = null;
        if (request.parentId() != null) {
            parent = commentRepository.findById(request.parentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Yorum bulunamadı: id=" + request.parentId()));
            if (parent.getParent() != null) {
                throw new InvalidReferenceException("Bir yanıta yanıt verilemez (2 seviye sınırı)");
            }
            if (!parent.getThread().getId().equals(thread.getId())) {
                throw new InvalidReferenceException("parentId farklı bir thread'e ait");
            }
        }
        Comment comment = Comment.builder()
                .thread(thread)
                .parent(parent)
                .body(request.body())
                .spoilerFlagged(request.spoilerFlagged())
                .status(CommentStatus.PUBLISHED)
                .authorId(authorId)
                .build();
        comment = commentRepository.save(comment);
        threadRepository.incrementCommentCount(thread.getId());
        return commentMapper.toResponse(comment, 0, List.of());
    }

    @Override
    @Transactional
    public void delete(Long userId, boolean moderator, Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Yorum bulunamadı: id=" + commentId));
        if (!moderator && !comment.getAuthorId().equals(userId)) {
            throw new AccessDeniedException("Bu yorumu silme yetkiniz yok");
        }
        comment.setStatus(CommentStatus.DELETED);
    }

    private Thread findPublishedThread(String slug) {
        return threadRepository.findBySlugAndStatus(slug, ThreadStatus.PUBLISHED)
                .orElseThrow(() -> new ResourceNotFoundException("Thread bulunamadı: slug=" + slug));
    }

    private CommentResponse toResponseWithReplies(Comment comment) {
        long replyCount = commentRepository.countByParent_Id(comment.getId());
        List<CommentResponse> replies = commentRepository
                .findByParent_IdOrderByCreatedAtAsc(comment.getId(), PageRequest.of(0, REPLIES_PREVIEW_SIZE))
                .stream()
                .map(reply -> commentMapper.toResponse(reply, 0, List.of()))
                .toList();
        return commentMapper.toResponse(comment, (int) replyCount, replies);
    }
}
