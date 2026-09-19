package com.example.fandoom_backend.community.service;

import com.example.fandoom_backend.blog.service.BlogService;
import com.example.fandoom_backend.common.dto.KeysetPageResponse;
import com.example.fandoom_backend.common.dto.PageResponse;
import com.example.fandoom_backend.common.exception.InvalidReferenceException;
import com.example.fandoom_backend.common.exception.ResourceNotFoundException;
import com.example.fandoom_backend.common.specification.KeysetSpecification;
import com.example.fandoom_backend.common.util.KeysetCursor;
import com.example.fandoom_backend.community.dto.AuthorSummary;
import com.example.fandoom_backend.community.dto.CommentRequest;
import com.example.fandoom_backend.community.dto.CommentResponse;
import com.example.fandoom_backend.community.entity.Comment;
import com.example.fandoom_backend.community.entity.CommentStatus;
import com.example.fandoom_backend.community.entity.CommentSubjectType;
import com.example.fandoom_backend.community.entity.Thread;
import com.example.fandoom_backend.community.entity.ThreadStatus;
import com.example.fandoom_backend.community.mapper.CommentMapper;
import com.example.fandoom_backend.community.repository.CommentLikeRepository;
import com.example.fandoom_backend.community.repository.CommentRepository;
import com.example.fandoom_backend.community.repository.ThreadRepository;
import com.example.fandoom_backend.community.specification.CommentSpecificationBuilder;
import com.example.fandoom_backend.series.service.EpisodeService;
import com.example.fandoom_backend.series.service.SeasonService;
import com.example.fandoom_backend.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
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
    private final UserProfileService userProfileService;
    // Cross-module doğrulama — subjectId'nin ilgili modülde gerçekten var
    // olduğunu kontrol etmek için (bkz. person/Cast'teki subjectType+subjectId
    // deseni). Interface üzerinden inject edilir, entity/repository'lerine
    // erişilmez.
    private final BlogService blogService;
    private final SeasonService seasonService;
    private final EpisodeService episodeService;

    @Override
    public PageResponse<CommentResponse> listForThread(String threadSlug, String sort, Long viewerId, Pageable pageable) {
        Thread thread = findPublishedThread(threadSlug);
        return listForSubject(CommentSubjectType.THREAD, thread.getId(), sort, viewerId, pageable);
    }

    @Override
    public PageResponse<CommentResponse> listForSubject(
            CommentSubjectType subjectType, Long subjectId, String sort, Long viewerId, Pageable pageable) {
        Page<Comment> page = "hot".equals(sort)
                ? commentRepository.findBySubjectTypeAndSubjectIdAndParentIsNullOrderByLikeCountDesc(
                        subjectType, subjectId, pageable)
                : commentRepository.findBySubjectTypeAndSubjectIdAndParentIsNullOrderByCreatedAtDesc(
                        subjectType, subjectId, pageable);

        return PageResponse.from(new PageImpl<>(enrich(page.getContent(), viewerId), pageable, page.getTotalElements()));
    }

    @Override
    public KeysetPageResponse<CommentResponse> listForThreadByCursor(
            String threadSlug, String sort, Long viewerId, String cursor, int size) {
        Thread thread = findPublishedThread(threadSlug);
        return listForSubjectByCursor(CommentSubjectType.THREAD, thread.getId(), sort, viewerId, cursor, size);
    }

    // Keyset: "hot" -> (likeCount DESC, id DESC), aksi halde (createdAt DESC, id DESC).
    @Override
    public KeysetPageResponse<CommentResponse> listForSubjectByCursor(
            CommentSubjectType subjectType, Long subjectId, String sort, Long viewerId, String cursor, int size) {
        boolean hot = "hot".equals(sort);
        String sortField = hot ? "likeCount" : "createdAt";
        Specification<Comment> specification = CommentSpecificationBuilder.topLevelOf(subjectType, subjectId);
        KeysetCursor decoded = KeysetCursor.decode(cursor);
        if (decoded != null) {
            specification = specification.and(afterCursor(sortField, decoded));
        }
        Sort order = Sort.by(Sort.Direction.DESC, sortField).and(Sort.by(Sort.Direction.DESC, "id"));
        List<Comment> rows = commentRepository.findBy(specification,
                q -> q.sortBy(order).limit(size + 1).all());

        boolean hasNext = rows.size() > size;
        List<Comment> pageRows = hasNext ? rows.subList(0, size) : rows;
        String nextCursor = null;
        if (hasNext) {
            Comment last = pageRows.get(pageRows.size() - 1);
            String value = hot ? String.valueOf(last.getLikeCount()) : last.getCreatedAt().toString();
            nextCursor = new KeysetCursor(value, last.getId()).encode();
        }
        return new KeysetPageResponse<>(enrich(pageRows, viewerId), hasNext, nextCursor);
    }

    private Specification<Comment> afterCursor(String sortField, KeysetCursor cursor) {
        try {
            return "likeCount".equals(sortField)
                    ? KeysetSpecification.<Comment, Integer>after(sortField, Integer.valueOf(cursor.value()), cursor.id())
                    : KeysetSpecification.<Comment, LocalDateTime>after(
                            sortField, LocalDateTime.parse(cursor.value()), cursor.id());
        } catch (NumberFormatException | DateTimeParseException e) {
            throw new InvalidReferenceException("Geçersiz cursor");
        }
    }

    // Sayfadaki üst seviye yorumları zenginleştirir. Yanıt sayıları + ilk 3 yanıt önizlemesi,
    // author (username/avatar) ve isLiked toplu çözülür: yorum sayısından bağımsız SABİT sayıda
    // sorgu (eski hali her yorum için ayrı count + reply sorgusu çalıştırıyordu -> 2N+1).
    private List<CommentResponse> enrich(List<Comment> parents, Long viewerId) {
        if (parents.isEmpty()) {
            return List.of();
        }
        List<Long> parentIds = parents.stream().map(Comment::getId).toList();

        Map<Long, Integer> replyCounts = new HashMap<>();
        for (Object[] row : commentRepository.countRepliesByParentIds(parentIds)) {
            replyCounts.put((Long) row[0], ((Number) row[1]).intValue());
        }
        Map<Long, List<Comment>> repliesByParent = commentRepository
                .findFirstRepliesByParentIds(parentIds, REPLIES_PREVIEW_SIZE).stream()
                .collect(Collectors.groupingBy(r -> r.getParent().getId()));

        Set<Long> authorIds = new HashSet<>();
        Set<Long> commentIds = new HashSet<>();
        for (Comment parent : parents) {
            authorIds.add(parent.getAuthorId());
            commentIds.add(parent.getId());
            for (Comment reply : repliesByParent.getOrDefault(parent.getId(), List.of())) {
                authorIds.add(reply.getAuthorId());
                commentIds.add(reply.getId());
            }
        }
        Map<Long, String> usernames = userService.getUsernamesByIds(authorIds);
        Map<Long, String> avatarUrls = userProfileService.getAvatarUrlsByUserIds(authorIds);
        Set<Long> likedCommentIds = (viewerId == null || commentIds.isEmpty())
                ? Set.of()
                : commentLikeRepository.findCommentIdsByUserIdAndCommentIdIn(viewerId, commentIds);

        return parents.stream()
                .map(parent -> toResponse(parent, replyCounts.getOrDefault(parent.getId(), 0),
                        repliesByParent.getOrDefault(parent.getId(), List.of()).stream()
                                .map(reply -> toResponse(reply, 0, List.of(), usernames, avatarUrls, likedCommentIds))
                                .toList(),
                        usernames, avatarUrls, likedCommentIds))
                .toList();
    }

    @Override
    @Transactional
    public CommentResponse create(Long authorId, String threadSlug, CommentRequest request) {
        Thread thread = findPublishedThread(threadSlug);
        return createForSubject(authorId, CommentSubjectType.THREAD, thread.getId(), request);
    }

    @Override
    @Transactional
    public CommentResponse createForSubject(
            Long authorId, CommentSubjectType subjectType, Long subjectId, CommentRequest request) {
        validateSubject(subjectType, subjectId);
        Comment parent = null;
        if (request.parentId() != null) {
            parent = commentRepository.findById(request.parentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Yorum bulunamadı: id=" + request.parentId()));
            if (parent.getParent() != null) {
                throw new InvalidReferenceException("Bir yanıta yanıt verilemez (2 seviye sınırı)");
            }
            if (parent.getSubjectType() != subjectType || !parent.getSubjectId().equals(subjectId)) {
                throw new InvalidReferenceException("parentId farklı bir konuya ait");
            }
        }
        Comment comment = Comment.builder()
                .subjectType(subjectType)
                .subjectId(subjectId)
                .parent(parent)
                .body(request.body())
                .spoilerFlagged(request.spoilerFlagged())
                .status(CommentStatus.PUBLISHED)
                .authorId(authorId)
                .build();
        comment = commentRepository.save(comment);
        // Sayaç sadece THREAD'de tutulur — BLOG/SEASON/EPISODE yorumu subjectId'yi
        // bir Thread ID sanıp alakasız bir Thread'in sayacını güncellemesin diye.
        if (subjectType == CommentSubjectType.THREAD) {
            threadRepository.incrementCommentCount(subjectId);
        }
        String username = userService.getUsernamesByIds(Set.of(authorId)).get(authorId);
        String avatarUrl = userProfileService.getAvatarUrlsByUserIds(Set.of(authorId)).get(authorId);
        return commentMapper.toResponse(comment, 0, List.of(), new AuthorSummary(authorId, username, avatarUrl), false);
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
            if (comment.getSubjectType() == CommentSubjectType.THREAD) {
                threadRepository.decrementCommentCount(comment.getSubjectId());
            }
        }
    }

    // BLOG/EPISODE.existsById zaten servis interface'inde vardı; SEASON'a bu
    // görev kapsamında eklendi (bkz. SeasonService). THREAD, community/'nin
    // kendi aggregate'i olduğu için doğrudan ThreadRepository ile PUBLISHED
    // kontrolü yapılır (DELETED bir thread'e yorum eklenemesin, mevcut
    // davranış korunur).
    private void validateSubject(CommentSubjectType subjectType, Long subjectId) {
        switch (subjectType) {
            case THREAD -> {
                Thread thread = threadRepository.findById(subjectId)
                        .orElseThrow(() -> new ResourceNotFoundException("Thread bulunamadı: id=" + subjectId));
                if (thread.getStatus() != ThreadStatus.PUBLISHED) {
                    throw new ResourceNotFoundException("Thread bulunamadı: id=" + subjectId);
                }
            }
            case BLOG -> {
                if (!blogService.existsById(subjectId)) {
                    throw new ResourceNotFoundException("Blog bulunamadı: id=" + subjectId);
                }
            }
            case SEASON -> {
                if (!seasonService.existsById(subjectId)) {
                    throw new ResourceNotFoundException("Season bulunamadı: id=" + subjectId);
                }
            }
            case EPISODE -> {
                if (!episodeService.existsById(subjectId)) {
                    throw new ResourceNotFoundException("Episode bulunamadı: id=" + subjectId);
                }
            }
        }
    }

    private Thread findPublishedThread(String slug) {
        return threadRepository.findBySlugAndStatus(slug, ThreadStatus.PUBLISHED)
                .orElseThrow(() -> new ResourceNotFoundException("Thread bulunamadı: slug=" + slug));
    }

    private CommentResponse toResponse(Comment comment, int replyCount, List<CommentResponse> replies,
                                        Map<Long, String> usernames, Map<Long, String> avatarUrls,
                                        Set<Long> likedCommentIds) {
        AuthorSummary author = new AuthorSummary(comment.getAuthorId(), usernames.get(comment.getAuthorId()),
                avatarUrls.get(comment.getAuthorId()));
        boolean liked = likedCommentIds.contains(comment.getId());
        return commentMapper.toResponse(comment, replyCount, replies, author, liked);
    }
}
