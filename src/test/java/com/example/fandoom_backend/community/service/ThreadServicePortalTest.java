package com.example.fandoom_backend.community.service;

import com.example.fandoom_backend.common.exception.InvalidReferenceException;
import com.example.fandoom_backend.common.exception.ResourceNotFoundException;
import com.example.fandoom_backend.community.dto.PortalRefResponse;
import com.example.fandoom_backend.community.dto.ThreadRequest;
import com.example.fandoom_backend.community.entity.Thread;
import com.example.fandoom_backend.community.entity.ThreadStatus;
import com.example.fandoom_backend.community.entity.ThreadSurface;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// ThreadServiceImpl'in Portal ile ilgili davranışları (id tabanlı okuma, portal doğrulama sırası, sayaçlar,
// portal filtresi, sayfa başına tek portal çözümü). Genel thread davranışı: ThreadServiceImplTest.
@ExtendWith(MockitoExtension.class)
class ThreadServicePortalTest {

    private static final Long PORTAL_ID = 5L;
    private static final PortalRefResponse PORTAL_REF = new PortalRefResponse("westeros", "Westeros");

    @Mock private ThreadRepository threadRepository;
    @Mock private ThreadTagRepository threadTagRepository;
    @Mock private ThreadLikeRepository threadLikeRepository;
    @Mock private ThreadBookmarkRepository threadBookmarkRepository;
    @Mock private ThreadMapper threadMapper;
    @Mock private MovieService movieService;
    @Mock private SeriesService seriesService;
    @Mock private UserService userService;
    @Mock private UserProfileService userProfileService;
    @Mock private ThreadMediaService threadMediaService;
    @Mock private PortalService portalService;

    private ThreadServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ThreadServiceImpl(threadRepository, threadTagRepository, threadLikeRepository,
                threadBookmarkRepository, threadMapper, movieService, seriesService, userService,
                userProfileService, threadMediaService, portalService);
        lenient().when(userService.getUsernamesByIds(any())).thenReturn(Map.of());
        lenient().when(userProfileService.getAvatarUrlsByUserIds(any())).thenReturn(Map.of());
        lenient().when(threadTagRepository.findByThread_Id(any())).thenReturn(List.of());
        lenient().when(threadTagRepository.findByThread_IdIn(any())).thenReturn(List.of());
        lenient().when(threadMediaService.getMedia(any())).thenReturn(List.of());
        lenient().when(threadMediaService.getMediaByThread(any())).thenReturn(Map.of());
        lenient().when(threadMediaService.replace(any(), any())).thenReturn(List.of());
        lenient().when(portalService.getRefsByIds(any())).thenReturn(Map.of(PORTAL_ID, PORTAL_REF));
        lenient().when(threadRepository.save(any(Thread.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private Thread thread(Long id) {
        return Thread.builder().id(id).slug("t" + id).surface(ThreadSurface.DISCUSSION).title("Başlık")
                .status(ThreadStatus.PUBLISHED).authorId(1L).portalId(PORTAL_ID).build();
    }

    private static ThreadRequest request(String portalSlug, String productionSlug) {
        return new ThreadRequest(ThreadSurface.DISCUSSION, "Yeterince Uzun Başlık", "gövde", null, false,
                productionSlug, null, null, portalSlug);
    }

    // ---- create ----

    @Test
    void create_resolvesPortal_setsPortalId_incrementsThreadCount_andReturnsPortalRef() {
        when(portalService.resolvePostingPortalId("westeros", false)).thenReturn(PORTAL_ID);

        service.create(1L, false, request("westeros", null));

        ArgumentCaptor<Thread> saved = ArgumentCaptor.forClass(Thread.class);
        verify(threadRepository).save(saved.capture());
        assertThat(saved.getValue().getPortalId()).isEqualTo(PORTAL_ID);
        verify(portalService).incrementThreadCount(PORTAL_ID);
        verify(threadMapper).toDetailResponse(any(), any(), any(), eq(false), eq(false), any(), eq(PORTAL_REF));
    }

    @Test
    void create_passesModeratorFlagToPostingPolicyCheck() {
        when(portalService.resolvePostingPortalId("westeros", true)).thenReturn(PORTAL_ID);

        service.create(1L, true, request("westeros", null));

        verify(portalService).resolvePostingPortalId("westeros", true);
    }

    @Test
    void create_portalNotFound_propagates404_andNothingIsSaved() {
        when(portalService.resolvePostingPortalId("yok", false))
                .thenThrow(new ResourceNotFoundException("Portal bulunamadı"));

        assertThatThrownBy(() -> service.create(1L, false, request("yok", null)))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(threadRepository, never()).save(any());
        verify(portalService, never()).incrementThreadCount(any());
    }

    @Test
    void create_archivedPortal_propagates400() {
        when(portalService.resolvePostingPortalId("arsiv", false))
                .thenThrow(new InvalidReferenceException("Portal arşivlenmiş"));

        assertThatThrownBy(() -> service.create(1L, false, request("arsiv", null)))
                .isInstanceOf(InvalidReferenceException.class);

        verify(threadRepository, never()).save(any());
    }

    @Test
    void create_staffOnlyPortalAsNormalUser_propagates403() {
        when(portalService.resolvePostingPortalId("duyuru", false))
                .thenThrow(new AccessDeniedException("staff only"));

        assertThatThrownBy(() -> service.create(1L, false, request("duyuru", null)))
                .isInstanceOf(AccessDeniedException.class);

        verify(threadRepository, never()).save(any());
    }

    @Test
    void create_productionNotOfPortal_propagates400_beforeSaving() {
        when(portalService.resolvePostingPortalId("westeros", false)).thenReturn(PORTAL_ID);
        when(movieService.existsBySlug("stranger-things")).thenReturn(false);
        when(seriesService.existsBySlug("stranger-things")).thenReturn(true);
        org.mockito.Mockito.doThrow(new InvalidReferenceException("portalın yapımı değil"))
                .when(portalService).assertProductionBelongsToPortal(PORTAL_ID, "stranger-things");

        assertThatThrownBy(() -> service.create(1L, false, request("westeros", "stranger-things")))
                .isInstanceOf(InvalidReferenceException.class);

        verify(threadRepository, never()).save(any());
    }

    @Test
    void create_portalIsResolvedBeforeProductionSlugIsValidated() {
        when(portalService.resolvePostingPortalId("yok", false))
                .thenThrow(new ResourceNotFoundException("Portal bulunamadı"));

        assertThatThrownBy(() -> service.create(1L, false, request("yok", "bilinmeyen")))
                .isInstanceOf(ResourceNotFoundException.class);

        // portal 404'ü productionSlug 400'ünden önce gelir
        verify(movieService, never()).existsBySlug(any());
    }

    // ---- delete ----

    @Test
    void delete_publishedThread_decrementsPortalThreadCount() {
        Thread t = thread(10L);
        when(threadRepository.findWithLockBySlug("t10")).thenReturn(Optional.of(t));

        when(threadRepository.markDeletedIfPublished(eq(10L), any())).thenReturn(1);

        service.delete(1L, false, "t10");

        verify(portalService).decrementThreadCount(PORTAL_ID);
    }

    @Test
    void delete_alreadyDeletedThread_atomicUpdateAffectsNoRow_doesNotDecrementAgain() {
        Thread t = thread(10L);
        when(threadRepository.findWithLockBySlug("t10")).thenReturn(Optional.of(t));
        when(threadRepository.markDeletedIfPublished(eq(10L), any())).thenReturn(0);   // başka çağrı zaten sildi

        service.delete(1L, false, "t10");

        verify(portalService, never()).decrementThreadCount(any());
    }

    @Test
    void delete_hiddenThreadIsDeletedWithoutTouchingCounter() {
        Thread t = thread(10L);
        when(threadRepository.findWithLockBySlug("t10")).thenReturn(Optional.of(t));
        when(threadRepository.markDeletedIfPublished(eq(10L), any())).thenReturn(0);

        service.delete(1L, false, "t10");

        verify(threadRepository).markDeletedIfHidden(eq(10L), any());
        verify(portalService, never()).decrementThreadCount(any());
    }

    // ---- HIDDEN portal: sahip PATCH/DELETE edemez; moderatör edebilir ----

    @Test
    void patchAndDelete_threadInHiddenPortal_is404ForOwner_butModeratorPasses() {
        Thread t = thread(10L);
        when(threadRepository.findById(10L)).thenReturn(Optional.of(t));
        when(threadRepository.findWithLockById(10L)).thenReturn(Optional.of(t));   // delete kilitli okur
        when(threadRepository.isInHiddenPortal(10L)).thenReturn(true);
        var patch = new com.example.fandoom_backend.community.dto.ThreadPatchRequest(null, "yeni gövde", null, null, null, null, null);

        assertThatThrownBy(() -> service.update(1L, false, 10L, patch)).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.delete(1L, false, 10L)).isInstanceOf(ResourceNotFoundException.class);
        verify(threadRepository, never()).markDeletedIfPublished(any(), any());

        service.update(99L, true, 10L, patch);                       // moderasyon serbest
        service.delete(99L, true, 10L);
        verify(threadRepository).markDeletedIfPublished(eq(10L), any());
    }

    // ---- taşıma: kilitli okuma yalnız portalSlug varsa ----

    @Test
    void update_withPortalSlug_readsThreadWithPessimisticLock_withoutIt_plainRead() {
        Thread t = thread(10L);
        when(threadRepository.findWithLockById(10L)).thenReturn(Optional.of(t));
        when(threadRepository.findById(10L)).thenReturn(Optional.of(t));
        when(portalService.resolveMoveTargetId("hedef")).thenReturn(6L);

        service.update(99L, true, 10L, new com.example.fandoom_backend.community.dto.ThreadPatchRequest(
                null, null, null, null, null, null, "hedef"));
        verify(threadRepository).findWithLockById(10L);
        verify(threadRepository, never()).findById(10L);
        verify(portalService).moveThreadCount(PORTAL_ID, 6L);

        service.update(99L, true, 10L, new com.example.fandoom_backend.community.dto.ThreadPatchRequest(
                null, "gövde", null, null, null, null, null));
        verify(threadRepository).findById(10L);
    }

    @Test
    void patchRequest_blankPortalSlugIsTreatedAsNoMove() {
        var req = new com.example.fandoom_backend.community.dto.ThreadPatchRequest(null, null, null, null, null, null, "   ");
        assertThat(req.portalSlug()).isNull();
        assertThat(new com.example.fandoom_backend.community.dto.ThreadPatchRequest(null, null, null, null, null, null, "")
                .portalSlug()).isNull();
        assertThat(new com.example.fandoom_backend.community.dto.ThreadPatchRequest(null, null, null, null, null, null, " westeros ")
                .portalSlug()).isEqualTo("westeros");
    }

    // ---- getById / getBySlug ----

    @Test
    void getById_visibleThread_returnsDetailWithPortalRef() {
        Thread t = thread(10L);
        when(threadRepository.findVisibleById(10L)).thenReturn(Optional.of(t));

        service.getById(10L, null);

        verify(threadMapper).toDetailResponse(eq(t), any(), any(), eq(false), eq(false), any(), eq(PORTAL_REF));
    }

    @Test
    void getById_missingDeletedHiddenOrHiddenPortal_allLookLikeNotFound() {
        // findVisibleById: yok / DELETED / HIDDEN thread / portalı HIDDEN -> boş
        when(threadRepository.findVisibleById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(99L, null)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getBySlug_usesVisibilityAwareLookup() {
        when(threadRepository.findVisibleBySlug("gizli")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getBySlug("gizli", null)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getById_afterTitleChange_sameIdStillResolves() {
        // Kabul kriteri 3: başlık PATCH'lenince slug değişir, id sabit kalır (id tabanlı sorgu slug'a bakmaz).
        Thread t = thread(10L);
        when(threadRepository.findBySlug("t10")).thenReturn(Optional.of(t));
        when(threadRepository.existsBySlugAndIdNot(any(), eq(10L))).thenReturn(false);
        when(threadRepository.findVisibleById(10L)).thenReturn(Optional.of(t));

        service.update(1L, false, "t10", new com.example.fandoom_backend.community.dto.ThreadPatchRequest(
                "Tamamen Yeni Bir Başlık", null, null, null, null, null, null));
        assertThat(t.getSlug()).isNotEqualTo("t10");

        assertThat(service.getById(10L, null)).isNull(); // mapper mock'u null döner; önemli olan 404 atmaması
        verify(threadRepository, times(1)).findVisibleById(10L);
    }

    // ---- list ----

    @Test
    void list_withPortalSlug_resolvesVisiblePortal_andSkipsHiddenLookup() {
        when(portalService.resolveVisibleId("westeros")).thenReturn(PORTAL_ID);
        when(threadRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        service.list(ThreadSurface.THEORY, "westeros", null, null, "hot", null, PageRequest.of(0, 20));

        verify(portalService).resolveVisibleId("westeros");
        verify(portalService, never()).getHiddenPortalIds();
    }

    @Test
    void list_unknownOrHiddenPortalSlug_propagates404_withoutQueryingThreads() {
        when(portalService.resolveVisibleId("gizli")).thenThrow(new ResourceNotFoundException("Portal bulunamadı"));

        assertThatThrownBy(() -> service.list(null, "gizli", null, null, "hot", null, PageRequest.of(0, 20)))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(threadRepository, never()).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void list_withoutPortal_excludesHiddenPortals() {
        when(portalService.getHiddenPortalIds()).thenReturn(Set.of(9L));
        when(threadRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        service.list(null, null, null, null, "hot", null, PageRequest.of(0, 20));

        verify(portalService).getHiddenPortalIds();
    }

    @Test
    void list_blankPortalSlug_isTreatedAsNoPortalFilter() {
        when(threadRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        service.list(null, "  ", null, null, "hot", null, PageRequest.of(0, 20));

        verify(portalService, never()).resolveVisibleId(any());
        verify(portalService).getHiddenPortalIds();
    }

    @Test
    void list_resolvesPortalsForWholePageWithSingleBatchCall_noNPlusOne() {
        Thread a = thread(1L);
        Thread b = thread(2L);
        Thread c = thread(3L);
        when(threadRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(a, b, c)));

        service.list(null, null, null, null, "hot", null, PageRequest.of(0, 20));

        verify(portalService, times(1)).getRefsByIds(any());
        ArgumentCaptor<java.util.Collection<Long>> ids = ArgumentCaptor.forClass(java.util.Collection.class);
        verify(portalService).getRefsByIds(ids.capture());
        assertThat(ids.getValue()).containsExactly(PORTAL_ID);
        verify(threadMapper, times(3)).toSummaryResponse(any(), any(), any(), anyBoolean(), anyBoolean(), any(),
                eq(PORTAL_REF));
    }

    @Test
    void listByCursor_withPortalSlug_resolvesPortal() {
        when(portalService.resolveVisibleId("westeros")).thenReturn(PORTAL_ID);
        when(threadRepository.findBy(any(Specification.class), any())).thenReturn(List.<Thread>of());

        var result = service.listByCursor(null, "westeros", null, null, "new", null, null, 20);

        assertThat(result.content()).isEmpty();
        verify(portalService).resolveVisibleId("westeros");
    }

    // ---- ARCHIVED portal: yazma kapalı (PATCH) — sahip 400, moderatör serbest, DELETE açık ----

    @Test
    void patch_threadInArchivedPortal_is400ForOwner_butModeratorAndDeletePass() {
        Thread t = thread(10L);
        when(threadRepository.findById(10L)).thenReturn(Optional.of(t));
        when(threadRepository.findWithLockById(10L)).thenReturn(Optional.of(t));
        when(threadRepository.isInArchivedPortal(10L)).thenReturn(true);
        var patch = new com.example.fandoom_backend.community.dto.ThreadPatchRequest(null, "yeni gövde", null, null, null, null, null);

        assertThatThrownBy(() -> service.update(1L, false, 10L, patch))
                .isInstanceOf(com.example.fandoom_backend.common.exception.InvalidReferenceException.class);

        service.update(99L, true, 10L, patch);   // moderasyon serbest
        service.delete(1L, false, 10L);          // sahip kendi içeriğini silebilir
        verify(threadRepository).markDeletedIfPublished(eq(10L), any());
    }

    // ---- saf-sayısal başlık -> slug "-t" eki alır (rakam-only segment id sayılır; bkz. ThreadServiceImpl.threadSlug) ----

    @Test
    void create_numericOnlyTitle_slugGetsTSuffix_andCollisionStillNumbered() {
        when(portalService.resolvePostingPortalId("westeros", false)).thenReturn(PORTAL_ID);
        when(threadRepository.existsBySlug("2024202420")).thenReturn(false);
        when(threadRepository.existsBySlug("2024202420-t")).thenReturn(true);

        service.create(1L, false, new ThreadRequest(ThreadSurface.DISCUSSION, "2024202420", "gövde", null, false,
                null, null, null, "westeros"));

        ArgumentCaptor<Thread> saved = ArgumentCaptor.forClass(Thread.class);
        verify(threadRepository).save(saved.capture());
        assertThat(saved.getValue().getSlug()).isEqualTo("2024202420-t-2");
    }

    @Test
    void create_titleWithLetters_slugUnchanged() {
        when(portalService.resolvePostingPortalId("westeros", false)).thenReturn(PORTAL_ID);

        service.create(1L, false, new ThreadRequest(ThreadSurface.DISCUSSION, "12 Monkeys tartışması", "gövde", null,
                false, null, null, null, "westeros"));

        ArgumentCaptor<Thread> saved = ArgumentCaptor.forClass(Thread.class);
        verify(threadRepository).save(saved.capture());
        assertThat(saved.getValue().getSlug()).isEqualTo("12-monkeys-tartismasi");
    }
}
