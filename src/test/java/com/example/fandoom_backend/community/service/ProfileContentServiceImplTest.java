package com.example.fandoom_backend.community.service;

import com.example.fandoom_backend.account.dto.UserListSummaryResponse;
import com.example.fandoom_backend.account.entity.ListType;
import com.example.fandoom_backend.account.service.ProfileFollowService;
import com.example.fandoom_backend.account.service.UserListService;
import com.example.fandoom_backend.common.dto.PageResponse;
import com.example.fandoom_backend.common.exception.InvalidReferenceException;
import com.example.fandoom_backend.common.exception.ResourceNotFoundException;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfileContentServiceImplTest {

    private static final Long VIEWER = 99L;
    private static final Long ARYA = 42L;
    private static final Pageable PAGE = PageRequest.of(0, 20);

    @Mock
    private UserService userService;
    @Mock
    private ProfileFollowService profileFollowService;
    @Mock
    private ThreadService threadService;
    @Mock
    private UserListService userListService;
    @Mock
    private CommentRepository commentRepository;
    @Mock
    private ThreadRepository threadRepository;
    @Mock
    private PortalService portalService;
    @InjectMocks
    private ProfileContentServiceImpl service;

    // ---- threads (author filtresi) ----

    @Test
    void threads_blankAuthor_isBadRequestBeforeAnyLookup() {
        assertThatThrownBy(() -> service.threads("  ", null, "hot", VIEWER, PAGE))
                .isInstanceOf(InvalidReferenceException.class);

        verify(userService, never()).getIdByUsername(any());
    }

    @Test
    void threads_unknownUsername_propagatesNotFound() {
        when(userService.getIdByUsername("yok")).thenThrow(new ResourceNotFoundException("yok"));

        assertThatThrownBy(() -> service.threads("yok", null, "hot", VIEWER, PAGE))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void threads_restrictedViewer_getsEmptyPageAndThreadsAreNeverQueried() {
        when(userService.getIdByUsername("arya")).thenReturn(ARYA);
        when(profileFollowService.isRestricted(VIEWER, ARYA)).thenReturn(true);

        PageResponse<ThreadSummaryResponse> result = service.threads("arya", null, "hot", VIEWER, PAGE);

        assertThat(result.content()).isEmpty();
        assertThat(result.totalElements()).isZero();
        verify(threadService, never()).listByAuthorId(any(), any(), any(), any(), any());
    }

    @Test
    void threads_allowedViewer_delegatesWithResolvedAuthorId() {
        PageResponse<ThreadSummaryResponse> expected = PageResponse.from(new PageImpl<>(List.of(), PAGE, 0));
        when(userService.getIdByUsername("arya")).thenReturn(ARYA);
        when(profileFollowService.isRestricted(VIEWER, ARYA)).thenReturn(false);
        when(threadService.listByAuthorId(ARYA, ThreadSurface.THEORY, "new", VIEWER, PAGE)).thenReturn(expected);

        assertThat(service.threads("arya", ThreadSurface.THEORY, "new", VIEWER, PAGE)).isSameAs(expected);
    }

    // ---- comments ----

    @Test
    void comments_restrictedViewer_getsEmptyPageWithoutQuerying() {
        when(userService.getIdByUsername("arya")).thenReturn(ARYA);
        when(profileFollowService.isRestricted(null, ARYA)).thenReturn(true);

        assertThat(service.comments("arya", null, PAGE).content()).isEmpty();

        verify(commentRepository, never()).findVisibleByAuthor(any(), any(), any(), any(), anyCollection(), any());
    }

    @Test
    void comments_noHiddenPortals_passesSentinelSoNotInNeverGetsAnEmptyList() {
        when(userService.getIdByUsername("arya")).thenReturn(ARYA);
        when(portalService.getHiddenPortalIds()).thenReturn(Set.of());
        when(commentRepository.findVisibleByAuthor(any(), any(), any(), any(), anyCollection(), any()))
                .thenReturn(new PageImpl<>(List.of(), PAGE, 0));

        service.comments("arya", VIEWER, PAGE);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<Long>> hidden = ArgumentCaptor.forClass(Collection.class);
        verify(commentRepository).findVisibleByAuthor(eq(ARYA), eq(CommentStatus.PUBLISHED),
                eq(CommentSubjectType.THREAD), eq(ThreadStatus.PUBLISHED), hidden.capture(), eq(PAGE));
        assertThat(hidden.getValue()).isNotEmpty();
    }

    @Test
    void comments_threadComment_carriesThreadInfo_blogCommentHasNullThreadFields() {
        Comment onThread = comment(1L, CommentSubjectType.THREAD, 10L);
        Comment onBlog = comment(2L, CommentSubjectType.BLOG, 77L);
        Thread thread = Thread.builder().id(10L).slug("kuzeyin-sirri").title("Kuzeyin sırrı")
                .surface(ThreadSurface.THEORY).build();
        when(userService.getIdByUsername("arya")).thenReturn(ARYA);
        when(portalService.getHiddenPortalIds()).thenReturn(Set.of(5L));
        when(commentRepository.findVisibleByAuthor(any(), any(), any(), any(), anyCollection(), any()))
                .thenReturn(new PageImpl<>(List.of(onThread, onBlog), PAGE, 2));
        when(threadRepository.findAllById(Set.of(10L))).thenReturn(List.of(thread));

        List<UserCommentResponse> rows = service.comments("arya", VIEWER, PAGE).content();

        assertThat(rows).hasSize(2);
        assertThat(rows.get(0).threadSlug()).isEqualTo("kuzeyin-sirri");
        assertThat(rows.get(0).threadTitle()).isEqualTo("Kuzeyin sırrı");
        assertThat(rows.get(0).surface()).isEqualTo(ThreadSurface.THEORY);
        assertThat(rows.get(1).threadSlug()).isNull();
        assertThat(rows.get(1).subjectType()).isEqualTo(CommentSubjectType.BLOG);
        assertThat(rows.get(1).subjectId()).isEqualTo(77L);
    }

    @Test
    void comments_onlyNonThreadComments_neverLoadsThreads() {
        when(userService.getIdByUsername("arya")).thenReturn(ARYA);
        when(portalService.getHiddenPortalIds()).thenReturn(Set.of());
        when(commentRepository.findVisibleByAuthor(any(), any(), any(), any(), anyCollection(), any()))
                .thenReturn(new PageImpl<>(List.of(comment(2L, CommentSubjectType.EPISODE, 3L)), PAGE, 1));

        service.comments("arya", VIEWER, PAGE);

        verify(threadRepository, never()).findAllById(any());
    }

    // ---- lists ----

    @Test
    void lists_restrictedViewer_getsEmptyList() {
        when(userService.getIdByUsername("arya")).thenReturn(ARYA);
        when(profileFollowService.isRestricted(VIEWER, ARYA)).thenReturn(true);

        assertThat(service.lists("arya", VIEWER)).isEmpty();

        verify(userListService, never()).listPublicCustomByUserId(any());
    }

    @Test
    void lists_allowedViewer_returnsPublicCustomLists() {
        List<UserListSummaryResponse> expected =
                List.of(new UserListSummaryResponse(1L, "Favoriler", null, ListType.CUSTOM, 3, true, true));
        when(userService.getIdByUsername("arya")).thenReturn(ARYA);
        when(userListService.listPublicCustomByUserId(ARYA)).thenReturn(expected);

        assertThat(service.lists("arya", VIEWER)).isEqualTo(expected);
    }

    @Test
    void pinnedLists_restrictedViewer_getsEmptyList_allowedViewerGetsPinned() {
        when(userService.getIdByUsername("arya")).thenReturn(ARYA);
        when(profileFollowService.isRestricted(null, ARYA)).thenReturn(true);
        when(profileFollowService.isRestricted(VIEWER, ARYA)).thenReturn(false);
        List<UserListSummaryResponse> pinned =
                List.of(new UserListSummaryResponse(1L, "Pin", null, ListType.CUSTOM, 1, true, true));
        when(userListService.listPublicPinnedByUserId(ARYA)).thenReturn(pinned);

        assertThat(service.pinnedLists("arya", null)).isEmpty();
        assertThat(service.pinnedLists("arya", VIEWER)).isEqualTo(pinned);
    }

    private Comment comment(Long id, CommentSubjectType type, Long subjectId) {
        Comment c = Comment.builder().id(id).subjectType(type).subjectId(subjectId).body("metin")
                .authorId(ARYA).status(CommentStatus.PUBLISHED).build();
        c.setCreatedAt(LocalDateTime.of(2026, 10, 20, 12, 0));
        return c;
    }
}
