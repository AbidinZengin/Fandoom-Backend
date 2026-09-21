package com.example.fandoom_backend.community.service;

import com.example.fandoom_backend.blog.service.BlogService;
import com.example.fandoom_backend.common.dto.PageResponse;
import com.example.fandoom_backend.common.exception.ResourceNotFoundException;
import com.example.fandoom_backend.community.dto.CommentRequest;
import com.example.fandoom_backend.community.dto.CommentResponse;
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

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Thread yorumları: id varyantları (birincil) + HIDDEN portaldaki thread'in yorumlarının sızmaması.
@ExtendWith(MockitoExtension.class)
class CommentServiceThreadVisibilityTest {

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
    }

    private Thread thread(Long id) {
        return Thread.builder().id(id).status(ThreadStatus.PUBLISHED).portalId(1L).build();
    }

    @Test
    void listForThreadById_visibleThread_queriesThreadSubjectComments() {
        when(threadRepository.findVisibleById(7L)).thenReturn(Optional.of(thread(7L)));
        Pageable pageable = PageRequest.of(0, 10);
        when(commentRepository.findBySubjectTypeAndSubjectIdAndParentIsNullOrderByCreatedAtDesc(
                CommentSubjectType.THREAD, 7L, pageable)).thenReturn(new PageImpl<>(List.of()));

        PageResponse<CommentResponse> result = service.listForThread(7L, "new", null, pageable);

        assertThat(result.content()).isEmpty();
        verify(commentRepository).findBySubjectTypeAndSubjectIdAndParentIsNullOrderByCreatedAtDesc(
                CommentSubjectType.THREAD, 7L, pageable);
    }

    @Test
    void listForThreadById_invisibleThread_is404_withoutQueryingComments() {
        when(threadRepository.findVisibleById(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.listForThread(7L, "new", null, PageRequest.of(0, 10)))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(commentRepository, never())
                .findBySubjectTypeAndSubjectIdAndParentIsNullOrderByCreatedAtDesc(any(), any(), any());
    }

    @Test
    void listForThreadByIdCursor_invisibleThread_is404() {
        when(threadRepository.findVisibleById(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.listForThread(7L, "new", null, null, 20))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createForThreadId_invisibleThread_is404_andNothingSaved() {
        when(threadRepository.findVisibleById(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(1L, 7L,
                new CommentRequest("Yeterince uzun bir yorum metni", false, null)))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(commentRepository, never()).save(any());
        verify(threadRepository, never()).incrementCommentCount(any());
    }

    @Test
    void centralListForThreadSubject_inHiddenPortal_is404() {
        when(threadRepository.isInHiddenPortal(7L)).thenReturn(true);

        assertThatThrownBy(() -> service.listForSubject(
                CommentSubjectType.THREAD, 7L, "new", null, PageRequest.of(0, 10)))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.listForSubjectByCursor(
                CommentSubjectType.THREAD, 7L, "new", null, null, 20))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void centralListForNonThreadSubject_doesNotCheckThreadPortal() {
        Pageable pageable = PageRequest.of(0, 10);
        when(commentRepository.findBySubjectTypeAndSubjectIdAndParentIsNullOrderByCreatedAtDesc(
                CommentSubjectType.BLOG, 7L, pageable)).thenReturn(new PageImpl<>(List.of()));

        service.listForSubject(CommentSubjectType.BLOG, 7L, "new", null, pageable);

        verify(threadRepository, never()).isInHiddenPortal(any());
    }
}
