package com.example.fandoom_backend.community.service;

import com.cloudinary.Cloudinary;
import com.example.fandoom_backend.common.exception.InvalidReferenceException;
import com.example.fandoom_backend.community.dto.ThreadMediaRequest;
import com.example.fandoom_backend.community.dto.ThreadMediaResponse;
import com.example.fandoom_backend.community.entity.Thread;
import com.example.fandoom_backend.community.entity.ThreadMedia;
import com.example.fandoom_backend.community.entity.ThreadMediaType;
import com.example.fandoom_backend.community.repository.ThreadMediaRepository;
import com.example.fandoom_backend.media.service.CloudinaryMediaUrlValidator;
import com.example.fandoom_backend.media.service.ImageStorageService;
import com.example.fandoom_backend.media.service.VideoStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// URL doğrulaması gerçek CloudinaryMediaUrlValidator ile (cloud adı "demo"), storage'lar mock.
// Transaction senkronizasyonu aktif olmadığı için storage silmesi hemen çalışır.
@ExtendWith(MockitoExtension.class)
class ThreadMediaServiceImplTest {

    private static final String IMG_URL = "https://res.cloudinary.com/demo/image/upload/v1/fandoom/community/a.webp";
    private static final String IMG2_URL = "https://res.cloudinary.com/demo/image/upload/v1/fandoom/community/c.webp";
    private static final String VID_URL = "https://res.cloudinary.com/demo/video/upload/v1/fandoom/community/b.mp4";

    @Mock private ThreadMediaRepository threadMediaRepository;
    @Mock private ImageStorageService imageStorageService;
    @Mock private VideoStorageService videoStorageService;

    private ThreadMediaServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ThreadMediaServiceImpl(threadMediaRepository,
                new CloudinaryMediaUrlValidator(new Cloudinary("cloudinary://key:secret@demo")),
                imageStorageService, videoStorageService);
    }

    private Thread thread() {
        return Thread.builder().id(10L).build();
    }

    private static ThreadMediaRequest img(String url) {
        return new ThreadMediaRequest(ThreadMediaType.IMAGE, url);
    }

    private static ThreadMediaRequest vid(String url) {
        return new ThreadMediaRequest(ThreadMediaType.VIDEO, url);
    }

    private static ThreadMedia row(Thread thread, ThreadMediaType type, String url, int position) {
        return ThreadMedia.builder().thread(thread).type(type).url(url).position(position).build();
    }

    // ---- validation ----

    @Test
    void replace_sevenMedia_throwsInvalidReference() {
        List<ThreadMediaRequest> seven = IntStream.range(0, 7)
                .mapToObj(i -> img("https://res.cloudinary.com/demo/image/upload/v1/fandoom/community/" + i + ".webp"))
                .toList();

        assertThatThrownBy(() -> service.replace(thread(), seven)).isInstanceOf(InvalidReferenceException.class);

        verify(threadMediaRepository, never()).saveAll(any());
    }

    @Test
    void replace_sixMedia_isAccepted() {
        List<ThreadMediaRequest> six = IntStream.range(0, 6)
                .mapToObj(i -> img("https://res.cloudinary.com/demo/image/upload/v1/fandoom/community/" + i + ".webp"))
                .toList();

        assertThat(service.replace(thread(), six)).hasSize(6);
    }

    @Test
    void replace_wrongCloudName_throwsInvalidReference() {
        assertThatThrownBy(() -> service.replace(thread(),
                List.of(img("https://res.cloudinary.com/baskasi/image/upload/v1/x.webp"))))
                .isInstanceOf(InvalidReferenceException.class);
    }

    @Test
    void replace_foreignHost_throwsInvalidReference() {
        assertThatThrownBy(() -> service.replace(thread(),
                List.of(img("https://evil.example.com/demo/image/upload/v1/x.webp"))))
                .isInstanceOf(InvalidReferenceException.class);
        assertThatThrownBy(() -> service.replace(thread(),
                List.of(img("http://res.cloudinary.com/demo/image/upload/v1/x.webp"))))
                .isInstanceOf(InvalidReferenceException.class);
        assertThatThrownBy(() -> service.replace(thread(),
                List.of(img("https://res.cloudinary.com.evil.com/demo/image/upload/v1/x.webp"))))
                .isInstanceOf(InvalidReferenceException.class);
    }

    @Test
    void replace_typeMustMatchResourcePath() {
        assertThatThrownBy(() -> service.replace(thread(), List.of(vid(IMG_URL))))
                .isInstanceOf(InvalidReferenceException.class);
        assertThatThrownBy(() -> service.replace(thread(), List.of(img(VID_URL))))
                .isInstanceOf(InvalidReferenceException.class);
    }

    @Test
    void replace_queryOrFragmentInUrl_isRejected() {
        assertThatThrownBy(() -> service.replace(thread(), List.of(img(IMG_URL + "?x=1"))))
                .isInstanceOf(InvalidReferenceException.class);
    }

    // ---- persistence ----

    @Test
    @SuppressWarnings("unchecked")
    void replace_assignsPositionsInListOrder_andSyncsImageUrlToFirstImage() {
        Thread thread = thread();

        List<ThreadMediaResponse> result = service.replace(thread, List.of(vid(VID_URL), img(IMG_URL), img(IMG2_URL)));

        assertThat(result).extracting(ThreadMediaResponse::position).containsExactly(0, 1, 2);
        assertThat(result).extracting(ThreadMediaResponse::type)
                .containsExactly(ThreadMediaType.VIDEO, ThreadMediaType.IMAGE, ThreadMediaType.IMAGE);
        assertThat(thread.getImageUrl()).isEqualTo(IMG_URL);
        ArgumentCaptor<List<ThreadMedia>> captor = ArgumentCaptor.forClass(List.class);
        verify(threadMediaRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).extracting(ThreadMedia::getPosition).containsExactly(0, 1, 2);
    }

    @Test
    void replace_videoOnly_clearsImageUrl() {
        Thread thread = thread();
        thread.setImageUrl(IMG_URL);

        service.replace(thread, List.of(vid(VID_URL)));

        assertThat(thread.getImageUrl()).isNull();
    }

    // ---- storage cleanup ----

    @Test
    void replace_removedVideo_isDeletedWithVideoStorage() {
        Thread thread = thread();
        when(threadMediaRepository.findByThread_IdOrderByPositionAscIdAsc(10L))
                .thenReturn(List.of(row(thread, ThreadMediaType.IMAGE, IMG_URL, 0), row(thread, ThreadMediaType.VIDEO, VID_URL, 1)));

        service.replace(thread, List.of(img(IMG_URL)));

        verify(videoStorageService).delete(VID_URL);
        verify(imageStorageService, never()).delete(any());
    }

    @Test
    void replace_emptyList_deletesAllRemovedMedia() {
        Thread thread = thread();
        when(threadMediaRepository.findByThread_IdOrderByPositionAscIdAsc(10L))
                .thenReturn(List.of(row(thread, ThreadMediaType.IMAGE, IMG_URL, 0), row(thread, ThreadMediaType.VIDEO, VID_URL, 1)));

        service.replace(thread, List.of());

        verify(imageStorageService).delete(IMG_URL);
        verify(videoStorageService).delete(VID_URL);
    }

    @Test
    void replace_legacyImageUrlNotInNewList_isDeleted() {
        Thread thread = thread();
        thread.setImageUrl(IMG_URL);

        service.replace(thread, List.of(vid(VID_URL)));

        verify(imageStorageService).delete(IMG_URL);
    }

    @Test
    void replace_keptMedia_isNotDeleted() {
        Thread thread = thread();
        when(threadMediaRepository.findByThread_IdOrderByPositionAscIdAsc(10L))
                .thenReturn(List.of(row(thread, ThreadMediaType.IMAGE, IMG_URL, 0)));

        service.replace(thread, List.of(vid(VID_URL), img(IMG_URL)));

        verify(imageStorageService, never()).delete(any());
        verify(videoStorageService, never()).delete(any());
    }

    @Test
    void replace_removedUrlStillUsedByAnotherThread_isNotDeleted() {
        Thread thread = thread();
        when(threadMediaRepository.findByThread_IdOrderByPositionAscIdAsc(10L))
                .thenReturn(List.of(row(thread, ThreadMediaType.IMAGE, IMG_URL, 0)));
        when(threadMediaRepository.existsByUrl(IMG_URL)).thenReturn(true);

        service.replace(thread, List.of());

        verify(imageStorageService, never()).delete(any());
    }

    @Test
    void replace_removedUrlOutsideCommunityFolder_isNotDeleted() {
        Thread thread = thread();
        String poster = "https://res.cloudinary.com/demo/image/upload/v1/fandoom/movies/poster.webp";
        when(threadMediaRepository.findByThread_IdOrderByPositionAscIdAsc(10L))
                .thenReturn(List.of(row(thread, ThreadMediaType.IMAGE, poster, 0)));

        service.replace(thread, List.of());

        verify(imageStorageService, never()).delete(any());
    }

    @Test
    void replace_storageDeleteFailure_doesNotFailTheRequest() {
        Thread thread = thread();
        when(threadMediaRepository.findByThread_IdOrderByPositionAscIdAsc(10L))
                .thenReturn(List.of(row(thread, ThreadMediaType.VIDEO, VID_URL, 0)));
        doThrow(new java.io.UncheckedIOException(new java.io.IOException("cloudinary down")))
                .when(videoStorageService).delete(VID_URL);

        assertThat(service.replace(thread, List.of())).isEmpty();
    }

    // ---- read ----

    @Test
    void getMediaByThread_batchLoadsOnce_andFallsBackToLegacyImage() {
        Thread withRows = thread();
        Thread legacy = Thread.builder().id(11L).imageUrl(IMG_URL).build();
        Thread none = Thread.builder().id(12L).build();
        when(threadMediaRepository.findByThread_IdInOrderByPositionAscIdAsc(List.of(10L, 11L, 12L)))
                .thenReturn(List.of(row(withRows, ThreadMediaType.VIDEO, VID_URL, 0),
                        row(withRows, ThreadMediaType.IMAGE, IMG2_URL, 1)));

        Map<Long, List<ThreadMediaResponse>> result = service.getMediaByThread(List.of(withRows, legacy, none));

        assertThat(result.get(10L)).extracting(ThreadMediaResponse::url).containsExactly(VID_URL, IMG2_URL);
        assertThat(result.get(11L)).containsExactly(new ThreadMediaResponse(ThreadMediaType.IMAGE, IMG_URL, 0));
        assertThat(result.get(12L)).isEmpty();
    }

    @Test
    void getMediaByThread_emptyPage_runsNoQuery() {
        assertThat(service.getMediaByThread(List.of())).isEmpty();

        verify(threadMediaRepository, never()).findByThread_IdInOrderByPositionAscIdAsc(any());
    }
}
