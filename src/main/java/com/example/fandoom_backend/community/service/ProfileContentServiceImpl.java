package com.example.fandoom_backend.community.service;

import com.example.fandoom_backend.account.dto.UserListSummaryResponse;
import com.example.fandoom_backend.account.service.ProfileFollowService;
import com.example.fandoom_backend.account.service.UserListService;
import com.example.fandoom_backend.common.dto.PageResponse;
import com.example.fandoom_backend.common.exception.InvalidReferenceException;
import com.example.fandoom_backend.community.dto.ThreadSummaryResponse;
import com.example.fandoom_backend.community.dto.UserCommentResponse;
import com.example.fandoom_backend.community.entity.Comment;
import com.example.fandoom_backend.community.entity.CommentStatus;
import com.example.fandoom_backend.community.entity.CommentSubjectType;
import com.example.fandoom_backend.community.entity.Thread;
import com.example.fandoom_backend.community.entity.ThreadStatus;
import com.example.fandoom_backend.community.entity.ThreadSurface;
import com.example.fandoom_backend.community.repository.CommentRepository;
import com.example.fandoom_backend.community.repository.ThreadRepository;
import com.example.fandoom_backend.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProfileContentServiceImpl implements ProfileContentService {

    // NOT IN boş listeyle çalışmaz; hiç HIDDEN portal yokken bu sentinel hiçbir portal id'siyle eşleşmez.
    private static final long NO_PORTAL = -1L;

    private final UserService userService;
    private final ProfileFollowService profileFollowService;
    private final ThreadService threadService;
    private final UserListService userListService;
    private final CommentRepository commentRepository;
    private final ThreadRepository threadRepository;
    private final PortalService portalService;

    @Override
    public PageResponse<ThreadSummaryResponse> threads(
            String username, ThreadSurface surface, String sort, Long viewerId, Pageable pageable) {
        if (username == null || username.isBlank()) {
            throw new InvalidReferenceException("author boş olamaz");
        }
        Long authorId = userService.getIdByUsername(username.trim());
        if (profileFollowService.isRestricted(viewerId, authorId)) {
            return PageResponse.from(Page.empty(pageable));
        }
        return threadService.listByAuthorId(authorId, surface, sort, viewerId, pageable);
    }

    @Override
    public PageResponse<UserCommentResponse> comments(String username, Long viewerId, Pageable pageable) {
        Long authorId = userService.getIdByUsername(username);
        if (profileFollowService.isRestricted(viewerId, authorId)) {
            return PageResponse.from(Page.empty(pageable));
        }
        List<Long> hidden = new ArrayList<>(portalService.getHiddenPortalIds());
        if (hidden.isEmpty()) {
            hidden.add(NO_PORTAL);
        }
        Page<Comment> page = commentRepository.findVisibleByAuthor(authorId, CommentStatus.PUBLISHED,
                CommentSubjectType.THREAD, ThreadStatus.PUBLISHED, hidden, pageable);
        Map<Long, Thread> threads = threadsOf(page.getContent());
        return PageResponse.from(page.map(c -> toResponse(c, threads.get(c.getSubjectId()))));
    }

    @Override
    public List<UserListSummaryResponse> lists(String username, Long viewerId) {
        Long userId = userService.getIdByUsername(username);
        if (profileFollowService.isRestricted(viewerId, userId)) {
            return List.of();
        }
        return userListService.listPublicCustomByUserId(userId);
    }

    @Override
    public List<UserListSummaryResponse> pinnedLists(String username, Long viewerId) {
        Long userId = userService.getIdByUsername(username);
        if (profileFollowService.isRestricted(viewerId, userId)) {
            return List.of();
        }
        return userListService.listPublicPinnedByUserId(userId);
    }

    // Sayfa başına tek IN sorgusu (N+1 yok). Süzgeç sorguda uygulandığından bunlar hep yayındaki thread'lerdir.
    private Map<Long, Thread> threadsOf(Collection<Comment> comments) {
        Set<Long> threadIds = comments.stream()
                .filter(c -> c.getSubjectType() == CommentSubjectType.THREAD)
                .map(Comment::getSubjectId)
                .collect(Collectors.toSet());
        if (threadIds.isEmpty()) {
            return Map.of();
        }
        return threadRepository.findAllById(threadIds).stream()
                .collect(Collectors.toMap(Thread::getId, Function.identity()));
    }

    private UserCommentResponse toResponse(Comment comment, Thread thread) {
        return new UserCommentResponse(comment.getId(), comment.getBody(), comment.isSpoilerFlagged(),
                comment.getCreatedAt(), comment.getSubjectType(), comment.getSubjectId(),
                thread == null ? null : thread.getSlug(),
                thread == null ? null : thread.getTitle(),
                thread == null ? null : thread.getSurface());
    }
}
