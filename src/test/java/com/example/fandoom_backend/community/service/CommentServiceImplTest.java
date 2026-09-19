package com.example.fandoom_backend.community.service;

import com.example.fandoom_backend.blog.service.BlogService;
import com.example.fandoom_backend.common.dto.PageResponse;
import com.example.fandoom_backend.common.exception.InvalidReferenceException;
import com.example.fandoom_backend.common.exception.ResourceNotFoundException;
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
import com.example.fandoom_backend.series.service.EpisodeService;
import com.example.fandoom_backend.series.service.SeasonService;
import com.example.fandoom_backend.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Stil referansı: UserProfileServiceImplTest — MockitoExtension + @Mock alanlar,
// elle constructor injection, lenient() ortak stub'lar için.
@ExtendWith(MockitoExtension.class)
class CommentServiceImplTest {

    private static final Long AUTHOR_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;
    private static final CommentResponse DUMMY_RESPONSE = new CommentResponse(
            1L, CommentSubjectType.THREAD, 5L, 5L, null, "body", false, AUTHOR_ID,
            new AuthorSummary(AUTHOR_ID, "abidin", null), 0, false, 0, List.of(), null);

    @Mock private CommentRepository commentRepository;
    @Mock private ThreadRepository threadRepository;
    @Mock private CommentLikeRepository commentLikeRepository;
    @Mock private CommentMapper commentMapper;
    @Mock private UserService userService;
    @Mock private UserProfileService userProfileService;
    @Mock private BlogService blogService;
    @Mock private SeasonService seasonService;
    @Mock private EpisodeService episodeService;

    private CommentServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new CommentServiceImpl(commentRepository, threadRepository, commentLikeRepository,
                commentMapper, userService, userProfileService, blogService, seasonService, episodeService);

        lenient().when(userService.getUsernamesByIds(any())).thenReturn(Map.of());
        lenient().when(userProfileService.getAvatarUrlsByUserIds(any())).thenReturn(Map.of());
        lenient().when(commentRepository.save(any(Comment.class))).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(commentMapper.toResponse(any(), anyInt(), any(), any(), anyBoolean()))
                .thenReturn(DUMMY_RESPONSE);
    }

    private Thread publishedThread(Long id) {
        return Thread.builder().id(id).status(ThreadStatus.PUBLISHED).build();
    }

    // ---- validateSubject ----

    @Test
    void createForSubject_blogSubjectExists_savesComment() {
        when(blogService.existsById(42L)).thenReturn(true);
        CommentRequest request = new CommentRequest("Yeterince uzun bir yorum metni", false, null);

        service.createForSubject(AUTHOR_ID, CommentSubjectType.BLOG, 42L, request);

        verify(blogService).existsById(42L);
        verify(commentRepository).save(any(Comment.class));
    }

    @Test
    void createForSubject_blogSubjectMissing_throwsResourceNotFoundException() {
        when(blogService.existsById(42L)).thenReturn(false);
        CommentRequest request = new CommentRequest("Yeterince uzun bir yorum metni", false, null);

        assertThatThrownBy(() -> service.createForSubject(AUTHOR_ID, CommentSubjectType.BLOG, 42L, request))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(commentRepository, never()).save(any());
    }

    @Test
    void createForSubject_threadSubjectDeleted_throwsResourceNotFoundException() {
        Thread deletedThread = Thread.builder().id(5L).status(ThreadStatus.DELETED).build();
        when(threadRepository.findById(5L)).thenReturn(Optional.of(deletedThread));
        CommentRequest request = new CommentRequest("Yeterince uzun bir yorum metni", false, null);

        assertThatThrownBy(() -> service.createForSubject(AUTHOR_ID, CommentSubjectType.THREAD, 5L, request))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(commentRepository, never()).save(any());
    }

    @Test
    void createForSubject_threadSubjectPublished_savesCommentAndIncrementsCount() {
        when(threadRepository.findById(5L)).thenReturn(Optional.of(publishedThread(5L)));
        CommentRequest request = new CommentRequest("Yeterince uzun bir yorum metni", false, null);

        service.createForSubject(AUTHOR_ID, CommentSubjectType.THREAD, 5L, request);

        verify(threadRepository).incrementCommentCount(5L);
    }

    @Test
    void createForSubject_nonThreadSubject_neverTouchesThreadCommentCount() {
        when(blogService.existsById(42L)).thenReturn(true);
        CommentRequest request = new CommentRequest("Yeterince uzun bir yorum metni", false, null);

        service.createForSubject(AUTHOR_ID, CommentSubjectType.BLOG, 42L, request);

        verify(threadRepository, never()).incrementCommentCount(anyLong());
        verify(threadRepository, never()).decrementCommentCount(anyLong());
    }

    // ---- 2 seviye kısıtı ----

    @Test
    void createForSubject_replyToAReply_throwsInvalidReferenceException() {
        when(threadRepository.findById(5L)).thenReturn(Optional.of(publishedThread(5L)));
        Comment grandparent = Comment.builder().id(98L).subjectType(CommentSubjectType.THREAD).subjectId(5L).build();
        Comment replyParent = Comment.builder().id(99L).subjectType(CommentSubjectType.THREAD).subjectId(5L)
                .parent(grandparent).build();
        when(commentRepository.findById(99L)).thenReturn(Optional.of(replyParent));
        CommentRequest request = new CommentRequest("Yeterince uzun bir yorum metni", false, 99L);

        assertThatThrownBy(() -> service.createForSubject(AUTHOR_ID, CommentSubjectType.THREAD, 5L, request))
                .isInstanceOf(InvalidReferenceException.class);

        verify(commentRepository, never()).save(any());
    }

    @Test
    void createForSubject_parentBelongsToDifferentSubject_throwsInvalidReferenceException() {
        when(threadRepository.findById(5L)).thenReturn(Optional.of(publishedThread(5L)));
        Comment parentFromOtherThread = Comment.builder().id(50L)
                .subjectType(CommentSubjectType.THREAD).subjectId(999L).build();
        when(commentRepository.findById(50L)).thenReturn(Optional.of(parentFromOtherThread));
        CommentRequest request = new CommentRequest("Yeterince uzun bir yorum metni", false, 50L);

        assertThatThrownBy(() -> service.createForSubject(AUTHOR_ID, CommentSubjectType.THREAD, 5L, request))
                .isInstanceOf(InvalidReferenceException.class);

        verify(commentRepository, never()).save(any());
    }

    @Test
    void createForSubject_validTopLevelReply_isAccepted() {
        when(threadRepository.findById(5L)).thenReturn(Optional.of(publishedThread(5L)));
        Comment parent = Comment.builder().id(50L).subjectType(CommentSubjectType.THREAD).subjectId(5L).build();
        when(commentRepository.findById(50L)).thenReturn(Optional.of(parent));
        CommentRequest request = new CommentRequest("Yeterince uzun bir yorum metni", false, 50L);

        service.createForSubject(AUTHOR_ID, CommentSubjectType.THREAD, 5L, request);

        verify(commentRepository).save(any(Comment.class));
        verify(threadRepository).incrementCommentCount(5L);
    }

    // ---- delete ----

    @Test
    void delete_alreadyDeleted_isIdempotent_doesNotDecrementAgain() {
        Comment comment = Comment.builder().id(1L).authorId(AUTHOR_ID).status(CommentStatus.DELETED)
                .subjectType(CommentSubjectType.THREAD).subjectId(5L).build();
        when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));

        service.delete(AUTHOR_ID, false, 1L);

        assertThat(comment.getStatus()).isEqualTo(CommentStatus.DELETED);
        verify(threadRepository, never()).decrementCommentCount(anyLong());
    }

    @Test
    void delete_published_softDeletesAndDecrementsThreadCommentCount() {
        Comment comment = Comment.builder().id(2L).authorId(AUTHOR_ID).status(CommentStatus.PUBLISHED)
                .subjectType(CommentSubjectType.THREAD).subjectId(5L).build();
        when(commentRepository.findById(2L)).thenReturn(Optional.of(comment));

        service.delete(AUTHOR_ID, false, 2L);

        assertThat(comment.getStatus()).isEqualTo(CommentStatus.DELETED);
        verify(threadRepository).decrementCommentCount(5L);
    }

    @Test
    void delete_nonThreadSubject_neverTouchesThreadCommentCount() {
        Comment comment = Comment.builder().id(3L).authorId(AUTHOR_ID).status(CommentStatus.PUBLISHED)
                .subjectType(CommentSubjectType.BLOG).subjectId(42L).build();
        when(commentRepository.findById(3L)).thenReturn(Optional.of(comment));

        service.delete(AUTHOR_ID, false, 3L);

        verify(threadRepository, never()).decrementCommentCount(anyLong());
    }

    @Test
    void delete_notOwnerNotModerator_throwsAccessDeniedException() {
        Comment comment = Comment.builder().id(4L).authorId(AUTHOR_ID).status(CommentStatus.PUBLISHED)
                .subjectType(CommentSubjectType.THREAD).subjectId(5L).build();
        when(commentRepository.findById(4L)).thenReturn(Optional.of(comment));

        assertThatThrownBy(() -> service.delete(OTHER_USER_ID, false, 4L))
                .isInstanceOf(AccessDeniedException.class);

        assertThat(comment.getStatus()).isEqualTo(CommentStatus.PUBLISHED);
    }

    // ---- listForThread ----

    @Test
    void listForThread_resolvesSlugAndDelegatesToSubjectQuery() {
        Thread thread = publishedThread(7L);
        when(threadRepository.findBySlugAndStatus("slug", ThreadStatus.PUBLISHED)).thenReturn(Optional.of(thread));
        Pageable pageable = PageRequest.of(0, 10);
        when(commentRepository.findBySubjectTypeAndSubjectIdAndParentIsNullOrderByCreatedAtDesc(
                eq(CommentSubjectType.THREAD), eq(7L), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of()));

        PageResponse<CommentResponse> result = service.listForThread("slug", "new", null, pageable);

        verify(threadRepository).findBySlugAndStatus("slug", ThreadStatus.PUBLISHED);
        verify(commentRepository).findBySubjectTypeAndSubjectIdAndParentIsNullOrderByCreatedAtDesc(
                CommentSubjectType.THREAD, 7L, pageable);
        assertThat(result.content()).isEmpty();
    }

    @Test
    void listForThread_hotSort_usesLikeCountOrderedQuery() {
        Thread thread = publishedThread(7L);
        when(threadRepository.findBySlugAndStatus("slug", ThreadStatus.PUBLISHED)).thenReturn(Optional.of(thread));
        Pageable pageable = PageRequest.of(0, 10);
        when(commentRepository.findBySubjectTypeAndSubjectIdAndParentIsNullOrderByLikeCountDesc(
                eq(CommentSubjectType.THREAD), eq(7L), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of()));

        service.listForThread("slug", "hot", null, pageable);

        verify(commentRepository).findBySubjectTypeAndSubjectIdAndParentIsNullOrderByLikeCountDesc(
                CommentSubjectType.THREAD, 7L, pageable);
        verify(commentRepository, never())
                .findBySubjectTypeAndSubjectIdAndParentIsNullOrderByCreatedAtDesc(any(), any(), any());
    }

    // ---- N+1 regresyon koruması ----

    @Test
    void listForSubject_resolvesRepliesAndCountsInBatch_notPerComment() {
        Comment c1 = Comment.builder().id(1L).authorId(AUTHOR_ID).subjectType(CommentSubjectType.BLOG).subjectId(9L).build();
        Comment c2 = Comment.builder().id(2L).authorId(AUTHOR_ID).subjectType(CommentSubjectType.BLOG).subjectId(9L).build();
        Comment reply = Comment.builder().id(3L).authorId(OTHER_USER_ID).parent(c1)
                .subjectType(CommentSubjectType.BLOG).subjectId(9L).build();
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 20);
        when(commentRepository.findBySubjectTypeAndSubjectIdAndParentIsNullOrderByCreatedAtDesc(
                CommentSubjectType.BLOG, 9L, pageable))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(c1, c2), pageable, 2));
        when(commentRepository.countRepliesByParentIds(List.of(1L, 2L)))
                .thenReturn(java.util.Collections.singletonList(new Object[]{1L, 1L}));
        when(commentRepository.findFirstRepliesByParentIds(List.of(1L, 2L), 3)).thenReturn(List.of(reply));

        PageResponse<CommentResponse> result =
                service.listForSubject(CommentSubjectType.BLOG, 9L, "new", null, pageable);

        assertThat(result.content()).hasSize(2);
        verify(commentRepository).countRepliesByParentIds(List.of(1L, 2L));
        verify(commentRepository).findFirstRepliesByParentIds(List.of(1L, 2L), 3);
        verify(commentRepository, never()).countByParent_Id(any());
        verify(commentRepository, never()).findByParent_IdOrderByCreatedAtAsc(any(), any());
    }
}
