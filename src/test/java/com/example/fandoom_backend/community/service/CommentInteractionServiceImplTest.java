package com.example.fandoom_backend.community.service;

import com.example.fandoom_backend.community.dto.CommentLikeStatusResponse;
import com.example.fandoom_backend.community.entity.Comment;
import com.example.fandoom_backend.community.repository.CommentLikeRepository;
import com.example.fandoom_backend.community.repository.CommentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// ThreadInteractionServiceImplTest ile aynı idempotent toggle deseni.
@ExtendWith(MockitoExtension.class)
class CommentInteractionServiceImplTest {

    private static final Long USER_ID = 1L;
    private static final Long COMMENT_ID = 10L;

    @Mock private CommentRepository commentRepository;
    @Mock private CommentLikeRepository commentLikeRepository;
    @Mock private com.example.fandoom_backend.community.repository.ThreadRepository threadRepository;

    private CommentInteractionServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new CommentInteractionServiceImpl(commentRepository, commentLikeRepository, threadRepository);
    }

    private Comment commentWith(int likeCount) {
        Comment comment = Comment.builder().id(COMMENT_ID).likeCount(likeCount).build();
        when(commentRepository.findById(COMMENT_ID)).thenReturn(Optional.of(comment));
        return comment;
    }

    @Test
    void like_notAlreadyLiked_savesAndIncrementsCount() {
        commentWith(3);
        when(commentLikeRepository.existsByUserIdAndCommentId(USER_ID, COMMENT_ID)).thenReturn(false);

        CommentLikeStatusResponse response = service.like(USER_ID, COMMENT_ID);

        assertThat(response.liked()).isTrue();
        assertThat(response.likeCount()).isEqualTo(4);
        verify(commentLikeRepository).save(any());
        verify(commentRepository).incrementLikeCount(COMMENT_ID);
    }

    @Test
    void like_alreadyLiked_isIdempotent() {
        commentWith(3);
        when(commentLikeRepository.existsByUserIdAndCommentId(USER_ID, COMMENT_ID)).thenReturn(true);

        CommentLikeStatusResponse response = service.like(USER_ID, COMMENT_ID);

        assertThat(response.liked()).isTrue();
        assertThat(response.likeCount()).isEqualTo(3);
        verify(commentLikeRepository, never()).save(any());
        verify(commentRepository, never()).incrementLikeCount(COMMENT_ID);
    }

    @Test
    void unlike_wasLiked_deletesAndDecrementsCount() {
        commentWith(3);
        when(commentLikeRepository.existsByUserIdAndCommentId(USER_ID, COMMENT_ID)).thenReturn(true);

        CommentLikeStatusResponse response = service.unlike(USER_ID, COMMENT_ID);

        assertThat(response.liked()).isFalse();
        assertThat(response.likeCount()).isEqualTo(2);
        verify(commentLikeRepository).deleteByUserIdAndCommentId(USER_ID, COMMENT_ID);
        verify(commentRepository).decrementLikeCount(COMMENT_ID);
    }

    @Test
    void unlike_notLiked_isIdempotent() {
        commentWith(3);
        when(commentLikeRepository.existsByUserIdAndCommentId(USER_ID, COMMENT_ID)).thenReturn(false);

        CommentLikeStatusResponse response = service.unlike(USER_ID, COMMENT_ID);

        assertThat(response.liked()).isFalse();
        assertThat(response.likeCount()).isEqualTo(3);
        verify(commentLikeRepository, never()).deleteByUserIdAndCommentId(USER_ID, COMMENT_ID);
        verify(commentRepository, never()).decrementLikeCount(COMMENT_ID);
    }

    @Test
    void likeAndUnlike_onCommentOfThreadInHiddenPortal_is404() {
        Comment c = Comment.builder().id(COMMENT_ID).likeCount(1)
                .subjectType(com.example.fandoom_backend.community.entity.CommentSubjectType.THREAD).subjectId(55L).build();
        when(commentRepository.findById(COMMENT_ID)).thenReturn(Optional.of(c));
        when(threadRepository.isInHiddenPortal(55L)).thenReturn(true);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.like(USER_ID, COMMENT_ID))
                .isInstanceOf(com.example.fandoom_backend.common.exception.ResourceNotFoundException.class);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.unlike(USER_ID, COMMENT_ID))
                .isInstanceOf(com.example.fandoom_backend.common.exception.ResourceNotFoundException.class);
        verify(commentLikeRepository, never()).save(any());
    }
}
