package com.example.fandoom_backend.community.service;

import com.example.fandoom_backend.common.config.JpaAuditingConfig;
import com.example.fandoom_backend.community.dto.ThreadMediaRequest;
import com.example.fandoom_backend.community.dto.ThreadMediaResponse;
import com.example.fandoom_backend.community.entity.Thread;
import com.example.fandoom_backend.community.entity.ThreadMediaType;
import com.example.fandoom_backend.community.entity.ThreadStatus;
import com.example.fandoom_backend.community.entity.ThreadSurface;
import com.example.fandoom_backend.community.repository.ThreadMediaRepository;
import com.example.fandoom_backend.media.service.ImageStorageService;
import com.example.fandoom_backend.media.service.MediaUrlValidator;
import com.example.fandoom_backend.media.service.VideoStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

// Gerçek MySQL (HotScoreJobIntegrationTest ile aynı desen, test sonunda rollback): sil + yeniden ekle akışı,
// sıra korunumu, toplu okuma ve existsByUrl'nin bekleyen değişiklikleri flush ederek doğru sonuç vermesi.
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({JpaAuditingConfig.class, ThreadMediaServiceImpl.class})
class ThreadMediaServiceImplDataJpaTest {

    private static final String A = "https://res.cloudinary.com/demo/image/upload/v1/fandoom/community/tm-a.webp";
    private static final String B = "https://res.cloudinary.com/demo/video/upload/v1/fandoom/community/tm-b.mp4";
    private static final String C = "https://res.cloudinary.com/demo/image/upload/v1/fandoom/community/tm-c.webp";

    @Autowired private ThreadMediaServiceImpl service;
    @Autowired private ThreadMediaRepository threadMediaRepository;
    @Autowired private TestEntityManager entityManager;

    @MockitoBean private MediaUrlValidator mediaUrlValidator;
    @MockitoBean private ImageStorageService imageStorageService;
    @MockitoBean private VideoStorageService videoStorageService;

    @BeforeEach
    void allowAllUrls() {
        when(mediaUrlValidator.isOwnedImage(anyString())).thenReturn(true);
        when(mediaUrlValidator.isOwnedVideo(anyString())).thenReturn(true);
    }

    private Thread persistThread(String slug) {
        Thread thread = entityManager.persistFlushFind(Thread.builder()
                .slug(slug).surface(ThreadSurface.DISCUSSION).title("Medya testi başlığı").authorId(999_999L)
                .status(ThreadStatus.PUBLISHED).build());
        entityManager.clear();
        return thread;
    }

    private static ThreadMediaRequest img(String url) {
        return new ThreadMediaRequest(ThreadMediaType.IMAGE, url);
    }

    private static ThreadMediaRequest vid(String url) {
        return new ThreadMediaRequest(ThreadMediaType.VIDEO, url);
    }

    @Test
    void replace_thenRead_returnsMediaInListOrder_andReplaceCanReinsertSameUrl() {
        Thread thread = persistThread("tm-order");

        service.replace(thread, List.of(vid(B), img(A)));
        entityManager.flush();
        entityManager.clear();
        assertThat(service.getMedia(thread)).extracting(ThreadMediaResponse::url).containsExactly(B, A);
        assertThat(service.getMedia(thread)).extracting(ThreadMediaResponse::position).containsExactly(0, 1);

        // A yeniden ekleniyor, sıra değişiyor, C yeni: sil + yeniden ekle çakışma vermemeli
        Thread reloaded = entityManager.find(Thread.class, thread.getId());
        service.replace(reloaded, List.of(img(A), img(C)));
        entityManager.flush();
        entityManager.clear();

        assertThat(service.getMedia(thread)).extracting(ThreadMediaResponse::url).containsExactly(A, C);
        assertThat(entityManager.find(Thread.class, thread.getId()).getImageUrl()).isEqualTo(A);
    }

    @Test
    void replace_emptyList_removesAllRows_andClearsImageUrl() {
        Thread thread = persistThread("tm-empty");
        service.replace(thread, List.of(img(A), vid(B)));
        entityManager.flush();
        entityManager.clear();

        service.replace(entityManager.find(Thread.class, thread.getId()), List.of());
        entityManager.flush();
        entityManager.clear();

        assertThat(threadMediaRepository.findByThread_IdOrderByPositionAscIdAsc(thread.getId())).isEmpty();
        assertThat(entityManager.find(Thread.class, thread.getId()).getImageUrl()).isNull();
    }

    @Test
    void getMediaByThread_loadsWholePageInOneCall_perThreadOrdered() {
        Thread t1 = persistThread("tm-batch-1");
        Thread t2 = persistThread("tm-batch-2");
        Thread t3 = persistThread("tm-batch-3");
        service.replace(t1, List.of(img(A), vid(B)));
        service.replace(t2, List.of(img(C)));
        entityManager.flush();
        entityManager.clear();

        Map<Long, List<ThreadMediaResponse>> byThread = service.getMediaByThread(List.of(t1, t2, t3));

        assertThat(byThread.get(t1.getId())).extracting(ThreadMediaResponse::url).containsExactly(A, B);
        assertThat(byThread.get(t2.getId())).extracting(ThreadMediaResponse::url).containsExactly(C);
        assertThat(byThread.get(t3.getId())).isEmpty();
    }

    @Test
    void existsByUrl_seesPendingReplaceInSameTransaction() {
        Thread thread = persistThread("tm-exists");
        service.replace(thread, List.of(img(A)));
        entityManager.flush();
        assertThat(threadMediaRepository.existsByUrl(A)).isTrue();

        service.replace(entityManager.find(Thread.class, thread.getId()), List.of());

        // flush çağrılmadı: derived query auto-flush ile bekleyen silmeyi görmeli
        assertThat(threadMediaRepository.existsByUrl(A)).isFalse();
    }
}
