package com.example.fandoom_backend.community.service;

import com.example.fandoom_backend.common.dto.PageResponse;
import com.example.fandoom_backend.common.exception.InvalidReferenceException;
import com.example.fandoom_backend.community.dto.ThreadDetailResponse;
import com.example.fandoom_backend.community.dto.ThreadPatchRequest;
import com.example.fandoom_backend.community.dto.ThreadRequest;
import com.example.fandoom_backend.community.dto.ThreadSummaryResponse;
import com.example.fandoom_backend.community.entity.Thread;
import com.example.fandoom_backend.community.entity.ThreadStatus;
import com.example.fandoom_backend.community.entity.ThreadSurface;
import com.example.fandoom_backend.community.entity.ThreadTag;
import com.example.fandoom_backend.community.mapper.ThreadMapper;
import com.example.fandoom_backend.community.repository.ThreadBookmarkRepository;
import com.example.fandoom_backend.community.repository.ThreadLikeRepository;
import com.example.fandoom_backend.community.repository.ThreadRepository;
import com.example.fandoom_backend.community.repository.ThreadTagRepository;
import com.example.fandoom_backend.movie.service.MovieService;
import com.example.fandoom_backend.series.service.SeriesService;
import com.example.fandoom_backend.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Stil referansı: UserProfileServiceImplTest — MockitoExtension + @Mock alanlar,
// @InjectMocks yerine constructor injection ile elle `new ThreadServiceImpl(...)`,
// lenient() ortak stub'lar için, ArgumentCaptor davranış doğrulaması için.
@ExtendWith(MockitoExtension.class)
class ThreadServiceImplTest {

    private static final Long AUTHOR_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;
    private static final ThreadDetailResponse DUMMY_DETAIL = new ThreadDetailResponse(
            10L, "slug", ThreadSurface.DISCUSSION, "title", "body", null, false, AUTHOR_ID, null,
            null, 0, 0, 0, false, false, List.of(), null, null);
    private static final ThreadSummaryResponse DUMMY_SUMMARY = new ThreadSummaryResponse(
            10L, "slug", ThreadSurface.DISCUSSION, "title", "excerpt", null, false, AUTHOR_ID, null,
            null, 0, 0, 0, false, false, List.of(), null);

    @Mock private ThreadRepository threadRepository;
    @Mock private ThreadTagRepository threadTagRepository;
    @Mock private ThreadLikeRepository threadLikeRepository;
    @Mock private ThreadBookmarkRepository threadBookmarkRepository;
    @Mock private ThreadMapper threadMapper;
    @Mock private MovieService movieService;
    @Mock private SeriesService seriesService;
    @Mock private UserService userService;
    @Mock private UserProfileService userProfileService;

    private ThreadServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ThreadServiceImpl(threadRepository, threadTagRepository, threadLikeRepository,
                threadBookmarkRepository, threadMapper, movieService, seriesService, userService,
                userProfileService);

        lenient().when(userService.getUsernamesByIds(any())).thenReturn(Map.of());
        lenient().when(userProfileService.getAvatarUrlsByUserIds(any())).thenReturn(Map.of());
        lenient().when(threadTagRepository.findByThread_Id(any())).thenReturn(List.of());
        lenient().when(threadTagRepository.findByThread_IdIn(any())).thenReturn(List.of());
        lenient().when(threadMapper.toDetailResponse(any(), any(), any(), anyBoolean(), anyBoolean()))
                .thenReturn(DUMMY_DETAIL);
        lenient().when(threadMapper.toSummaryResponse(any(), any(), any(), anyBoolean(), anyBoolean()))
                .thenReturn(DUMMY_SUMMARY);
    }

    private Thread publishedThread() {
        return Thread.builder()
                .id(10L)
                .slug("eski-baslik")
                .surface(ThreadSurface.DISCUSSION)
                .title("Eski Başlık")
                .body("gövde")
                .status(ThreadStatus.PUBLISHED)
                .authorId(AUTHOR_ID)
                .likeCount(0)
                .commentCount(0)
                .bookmarkCount(0)
                .build();
    }

    // ---- create ----

    @Test
    void create_generatesSlugFromTitle() {
        ThreadRequest request = new ThreadRequest(ThreadSurface.DISCUSSION, "Yeni Bir Başlık Deneme",
                "gövde", null, false, null, null);
        when(threadRepository.existsBySlug(any())).thenReturn(false);
        when(threadRepository.save(any(Thread.class))).thenAnswer(inv -> inv.getArgument(0));

        service.create(AUTHOR_ID, request);

        ArgumentCaptor<Thread> captor = ArgumentCaptor.forClass(Thread.class);
        verify(threadRepository).save(captor.capture());
        assertThat(captor.getValue().getSlug()).isEqualTo("yeni-bir-baslik-deneme");
    }

    @Test
    void create_invalidProductionSlug_throwsInvalidReferenceException() {
        ThreadRequest request = new ThreadRequest(ThreadSurface.DISCUSSION, "Yeterince Uzun Bir Başlık",
                "gövde", null, false, "bilinmeyen-yapim", null);
        when(movieService.existsBySlug("bilinmeyen-yapim")).thenReturn(false);
        when(seriesService.existsBySlug("bilinmeyen-yapim")).thenReturn(false);

        assertThatThrownBy(() -> service.create(AUTHOR_ID, request))
                .isInstanceOf(InvalidReferenceException.class);

        verify(threadRepository, never()).save(any());
    }

    @Test
    void create_normalizesAndSavesTags() {
        ThreadRequest request = new ThreadRequest(ThreadSurface.THEORY, "Bir Teori Başlığı Burada",
                "gövde", null, false, null, List.of("Teori", "Teori", "Spoiler!"));
        when(threadRepository.existsBySlug(any())).thenReturn(false);
        when(threadRepository.save(any(Thread.class))).thenAnswer(inv -> inv.getArgument(0));

        service.create(AUTHOR_ID, request);

        ArgumentCaptor<ThreadTag> tagCaptor = ArgumentCaptor.forClass(ThreadTag.class);
        verify(threadTagRepository, times(2)).save(tagCaptor.capture());
        assertThat(tagCaptor.getAllValues()).extracting(ThreadTag::getTag)
                .containsExactly("teori", "spoiler");

        ArgumentCaptor<List<String>> tagsListCaptor = ArgumentCaptor.forClass(List.class);
        verify(threadMapper).toDetailResponse(any(), tagsListCaptor.capture(), any(), eq(false), eq(false));
        assertThat(tagsListCaptor.getValue()).containsExactly("teori", "spoiler");
    }

    // ---- update ----

    @Test
    void update_notOwnerNotModerator_throwsAccessDeniedException() {
        Thread thread = publishedThread();
        when(threadRepository.findBySlug("eski-baslik")).thenReturn(java.util.Optional.of(thread));
        ThreadPatchRequest request = new ThreadPatchRequest(null, "yeni gövde", null, null, null);

        assertThatThrownBy(() -> service.update(OTHER_USER_ID, false, "eski-baslik", request))
                .isInstanceOf(AccessDeniedException.class);

        assertThat(thread.getBody()).isEqualTo("gövde");
        verify(threadTagRepository, never()).deleteByThread_Id(any());
    }

    @Test
    void update_moderatorCanEditThreadTheyDontOwn() {
        Thread thread = publishedThread();
        when(threadRepository.findBySlug("eski-baslik")).thenReturn(java.util.Optional.of(thread));
        ThreadPatchRequest request = new ThreadPatchRequest(null, "moderatör düzenlemesi", null, null, null);

        service.update(OTHER_USER_ID, true, "eski-baslik", request);

        assertThat(thread.getBody()).isEqualTo("moderatör düzenlemesi");
    }

    @Test
    void update_titleChanged_regeneratesSlug() {
        Thread thread = publishedThread();
        when(threadRepository.findBySlug("eski-baslik")).thenReturn(java.util.Optional.of(thread));
        when(threadRepository.existsBySlugAndIdNot(any(), eq(10L))).thenReturn(false);
        ThreadPatchRequest request = new ThreadPatchRequest("Yeni Başlık Ile Güncelleme", null, null, null, null);

        service.update(AUTHOR_ID, false, "eski-baslik", request);

        assertThat(thread.getSlug()).isEqualTo("yeni-baslik-ile-guncelleme");
        assertThat(thread.getTitle()).isEqualTo("Yeni Başlık Ile Güncelleme");
    }

    @Test
    void update_tagsNull_preservesExistingTags() {
        Thread thread = publishedThread();
        when(threadRepository.findBySlug("eski-baslik")).thenReturn(java.util.Optional.of(thread));
        when(threadTagRepository.findByThread_Id(10L)).thenReturn(List.of(
                ThreadTag.builder().tag("mevcut").build()));
        ThreadPatchRequest request = new ThreadPatchRequest(null, null, null, null, null);

        service.update(AUTHOR_ID, false, "eski-baslik", request);

        verify(threadTagRepository, never()).deleteByThread_Id(any());
        ArgumentCaptor<List<String>> tagsListCaptor = ArgumentCaptor.forClass(List.class);
        verify(threadMapper).toDetailResponse(any(), tagsListCaptor.capture(), any(), anyBoolean(), anyBoolean());
        assertThat(tagsListCaptor.getValue()).containsExactly("mevcut");
    }

    @Test
    void update_tagsEmptyList_deletesAllTags() {
        Thread thread = publishedThread();
        when(threadRepository.findBySlug("eski-baslik")).thenReturn(java.util.Optional.of(thread));
        ThreadPatchRequest request = new ThreadPatchRequest(null, null, null, null, List.of());

        service.update(AUTHOR_ID, false, "eski-baslik", request);

        verify(threadTagRepository).deleteByThread_Id(10L);
        verify(threadTagRepository, never()).save(any());
        ArgumentCaptor<List<String>> tagsListCaptor = ArgumentCaptor.forClass(List.class);
        verify(threadMapper).toDetailResponse(any(), tagsListCaptor.capture(), any(), anyBoolean(), anyBoolean());
        assertThat(tagsListCaptor.getValue()).isEmpty();
    }

    // ---- delete ----

    @Test
    void delete_softDeletesStatus_notHardDelete() {
        Thread thread = publishedThread();
        when(threadRepository.findBySlug("eski-baslik")).thenReturn(java.util.Optional.of(thread));

        service.delete(AUTHOR_ID, false, "eski-baslik");

        assertThat(thread.getStatus()).isEqualTo(ThreadStatus.DELETED);
        verify(threadRepository, never()).delete(any(Thread.class));
        verify(threadRepository, never()).deleteById(anyLong());
    }

    @Test
    void delete_notOwnerNotModerator_throwsAccessDeniedException() {
        Thread thread = publishedThread();
        when(threadRepository.findBySlug("eski-baslik")).thenReturn(java.util.Optional.of(thread));

        assertThatThrownBy(() -> service.delete(OTHER_USER_ID, false, "eski-baslik"))
                .isInstanceOf(AccessDeniedException.class);

        assertThat(thread.getStatus()).isEqualTo(ThreadStatus.PUBLISHED);
    }

    // ---- list ----

    @Test
    void list_withTags_callsFindByTagIn() {
        Thread thread = publishedThread();
        when(threadTagRepository.findByTagIn(List.of("teori"))).thenReturn(List.of(
                ThreadTag.builder().thread(thread).tag("teori").build()));
        when(threadRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(thread)));

        PageResponse<ThreadSummaryResponse> result = service.list(
                null, null, List.of("Teori"), "new", null, PageRequest.of(0, 10));

        verify(threadTagRepository).findByTagIn(List.of("teori"));
        assertThat(result.content()).hasSize(1);
    }

    @Test
    void list_withTags_emptyMatch_returnsEmptyPageResponseWithoutQueryingThreads() {
        when(threadTagRepository.findByTagIn(List.of("bilinmeyen"))).thenReturn(List.of());

        PageResponse<ThreadSummaryResponse> result = service.list(
                null, null, List.of("bilinmeyen"), "new", null, PageRequest.of(0, 10));

        assertThat(result.content()).isEmpty();
        assertThat(result.totalElements()).isZero();
        verify(threadRepository, never()).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void list_viewerNull_neverQueriesLikeOrBookmarkBulkLookup() {
        Thread thread = publishedThread();
        when(threadRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(thread)));

        service.list(null, null, null, "new", null, PageRequest.of(0, 10));

        verify(threadLikeRepository, never()).findThreadIdsByUserIdAndThreadIdIn(any(), any());
        verify(threadBookmarkRepository, never()).findThreadIdsByUserIdAndThreadIdIn(any(), any());
    }

    @Test
    void list_viewerPresent_usesBulkLikeAndBookmarkLookup() {
        Thread thread = publishedThread();
        when(threadRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(thread)));
        when(threadLikeRepository.findThreadIdsByUserIdAndThreadIdIn(eq(AUTHOR_ID), any()))
                .thenReturn(java.util.Set.of(10L));
        when(threadBookmarkRepository.findThreadIdsByUserIdAndThreadIdIn(eq(AUTHOR_ID), any()))
                .thenReturn(java.util.Set.of());

        service.list(null, null, null, "new", AUTHOR_ID, PageRequest.of(0, 10));

        verify(threadLikeRepository).findThreadIdsByUserIdAndThreadIdIn(eq(AUTHOR_ID), any());
        verify(threadBookmarkRepository).findThreadIdsByUserIdAndThreadIdIn(eq(AUTHOR_ID), any());
        verify(threadLikeRepository, never()).existsByUserIdAndThreadId(any(), any());
    }

    // ---- getBySlug ----

    @Test
    void getBySlug_viewerNull_neverQueriesLikeOrBookmark() {
        Thread thread = publishedThread();
        when(threadRepository.findBySlugAndStatus("eski-baslik", ThreadStatus.PUBLISHED))
                .thenReturn(java.util.Optional.of(thread));

        service.getBySlug("eski-baslik", null);

        verify(threadLikeRepository, never()).existsByUserIdAndThreadId(any(), any());
        verify(threadBookmarkRepository, never()).existsByUserIdAndThreadId(any(), any());
    }

    @Test
    void getBySlug_viewerPresent_queriesLikeAndBookmarkStatus() {
        Thread thread = publishedThread();
        when(threadRepository.findBySlugAndStatus("eski-baslik", ThreadStatus.PUBLISHED))
                .thenReturn(java.util.Optional.of(thread));

        service.getBySlug("eski-baslik", AUTHOR_ID);

        verify(threadLikeRepository).existsByUserIdAndThreadId(AUTHOR_ID, 10L);
        verify(threadBookmarkRepository).existsByUserIdAndThreadId(AUTHOR_ID, 10L);
    }
}
