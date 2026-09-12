package com.example.fandoom_backend.community.service;

import com.example.fandoom_backend.common.dto.PageResponse;
import com.example.fandoom_backend.common.exception.InvalidReferenceException;
import com.example.fandoom_backend.common.exception.ResourceNotFoundException;
import com.example.fandoom_backend.community.dto.AuthorSummary;
import com.example.fandoom_backend.community.dto.CommentRequest;
import com.example.fandoom_backend.community.dto.CommentResponse;
import com.example.fandoom_backend.community.entity.Comment;
import com.example.fandoom_backend.community.entity.CommentStatus;
import com.example.fandoom_backend.community.entity.Thread;
import com.example.fandoom_backend.community.entity.ThreadStatus;
import com.example.fandoom_backend.community.mapper.CommentMapper;
import com.example.fandoom_backend.community.repository.CommentLikeRepository;
import com.example.fandoom_backend.community.repository.CommentRepository;
import com.example.fandoom_backend.community.repository.ThreadRepository;
import com.example.fandoom_backend.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentServiceImpl implements CommentService {

    private static final int REPLIES_PREVIEW_SIZE = 3;

    private final CommentRepository commentRepository;
    private final ThreadRepository threadRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final CommentMapper commentMapper;
    private final UserService userService;

    @Override
    public PageResponse<CommentResponse> listForThread(String threadSlug, String sort, Long viewerId, Pageable pageable) {
        Thread thread = findPublishedThread(threadSlug);
        Page<Comment> page = "hot".equals(sort)
                ? commentRepository.findByThread_IdAndParentIsNullOrderByLikeCountDesc(thread.getId(), pageable)
                : commentRepository.findByThread_IdAndParentIsNullOrderByCreatedAtDesc(thread.getId(), pageable);

        // Yanıt önizlemeleri (max 3/yorum) önceden tek seferde çekilir ki
        // author/isLiked zenginleştirmesi tüm sayfa için tek toplu sorguyla yapılabilsin.
        List<CommentWithReplies> enriched = page.getContent().stream()
                .map(comment -> new CommentWithReplies(
                        comment,
                        (int) commentRepository.countByParent_Id(comment.getId()),
                        commentRepository.findByParent_IdOrderByCreatedAtAsc(
                                comment.getId(), PageRequest.of(0, REPLIES_PREVIEW_SIZE))))
                .toList();

        Set<Long> authorIds = new HashSet<>();
        Set<Long> commentIds = new HashSet<>();
        for (CommentWithReplies e : enriched) {
            authorIds.add(e.comment().getAuthorId());
            commentIds.add(e.comment().getId());
            for (Comment reply : e.replies()) {
                authorIds.add(reply.getAuthorId());
                commentIds.add(reply.getId());
            }
        }
        Map<Long, String> usernames = userService.getUsernamesByIds(authorIds);
        Set<Long> likedCommentIds = (viewerId == null || commentIds.isEmpty())
                ? Set.of()
                : commentLikeRepository.findCommentIdsByUserIdAndCommentIdIn(viewerId, commentIds);

        List<CommentResponse> content = enriched.stream()
                .map(e -> toResponse(e.comment(), e.replyCount(),
                        e.replies().stream().map(reply -> toResponse(reply, 0, List.of(), usernames, likedCommentIds)).toList(),
                        usernames, likedCommentIds))
                .toList();

        return PageResponse.from(new PageImpl<>(content, pageable, page.getTotalElements()));
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
        String username = userService.getUsernamesByIds(Set.of(authorId)).get(authorId);
        return commentMapper.toResponse(comment, 0, List.of(), new AuthorSummary(authorId, username), false);
    }

    @Override
    @Transactional
    public void delete(Long userId, boolean moderator, Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Yorum bulunamadı: id=" + commentId));
        if (!moderator && !comment.getAuthorId().equals(userId)) {
            throw new AccessDeniedException("Bu yorumu silme yetkiniz yok");
        }
        // Idempotent: zaten silinmiş bir yoruma tekrar DELETE isteği sayaç
        // fazladan düşürmesin.
        if (comment.getStatus() != CommentStatus.DELETED) {
            comment.setStatus(CommentStatus.DELETED);
            threadRepository.decrementCommentCount(comment.getThread().getId());
        }
    }

    @Override
    public long countByAuthorId(Long authorId) {
        return commentRepository.countByAuthorIdAndStatus(authorId, CommentStatus.PUBLISHED);
    }

    private Thread findPublishedThread(String slug) {
        return threadRepository.findBySlugAndStatus(slug, ThreadStatus.PUBLISHED)
                .orElseThrow(() -> new ResourceNotFoundException("Thread bulunamadı: slug=" + slug));
    }

    private CommentResponse toResponse(Comment comment, int replyCount, List<CommentResponse> replies,
                                        Map<Long, String> usernames, Set<Long> likedCommentIds) {
        AuthorSummary author = new AuthorSummary(comment.getAuthorId(), usernames.get(comment.getAuthorId()));
        boolean liked = likedCommentIds.contains(comment.getId());
        return commentMapper.toResponse(comment, replyCount, replies, author, liked);
    }

    private record CommentWithReplies(Comment comment, int replyCount, List<Comment> replies) {
    }
}
