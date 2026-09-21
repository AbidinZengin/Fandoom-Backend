package com.example.fandoom_backend.community.service;

import com.example.fandoom_backend.common.exception.InvalidReferenceException;
import com.example.fandoom_backend.common.exception.ResourceNotFoundException;
import com.example.fandoom_backend.community.dto.PortalRefResponse;
import com.example.fandoom_backend.community.entity.Portal;
import com.example.fandoom_backend.community.entity.PortalPostingPolicy;
import com.example.fandoom_backend.community.entity.PortalStatus;
import com.example.fandoom_backend.community.mapper.PortalMapperImpl;
import com.example.fandoom_backend.community.repository.PortalMembershipRepository;
import com.example.fandoom_backend.community.repository.PortalProductionRepository;
import com.example.fandoom_backend.community.repository.PortalRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PortalServiceImplTest {

    @Mock private PortalRepository portalRepository;
    @Mock private PortalProductionRepository portalProductionRepository;
    @Mock private PortalMembershipRepository portalMembershipRepository;

    private PortalServiceImpl service;

    @BeforeEach
    void setUp() {
        // Gerçek (MapStruct üretimi) mapper: locale çözümü (LocalizedTextResolver) de bu testte doğrulanır.
        service = new PortalServiceImpl(portalRepository, portalProductionRepository, portalMembershipRepository, new PortalMapperImpl());
    }

    @AfterEach
    void resetLocale() {
        LocaleContextHolder.resetLocaleContext();
    }

    private Portal portal(String slug, PortalStatus status, PortalPostingPolicy policy) {
        return Portal.builder().id(7L).slug(slug).nameTr("Türkçe Ad").nameEn("English Name")
                .status(status).postingPolicy(policy).build();
    }

    // ---- resolveVisibleId ----

    @Test
    void resolveVisibleId_activeAndArchived_areReadable() {
        when(portalRepository.findBySlug("a")).thenReturn(Optional.of(portal("a", PortalStatus.ACTIVE, PortalPostingPolicy.OPEN)));
        when(portalRepository.findBySlug("b")).thenReturn(Optional.of(portal("b", PortalStatus.ARCHIVED, PortalPostingPolicy.OPEN)));

        assertThat(service.resolveVisibleId("a")).isEqualTo(7L);
        assertThat(service.resolveVisibleId("b")).isEqualTo(7L);
    }

    @Test
    void resolveVisibleId_hiddenAndMissing_areNotFound() {
        when(portalRepository.findBySlug("h")).thenReturn(Optional.of(portal("h", PortalStatus.HIDDEN, PortalPostingPolicy.OPEN)));
        when(portalRepository.findBySlug("yok")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.resolveVisibleId("h")).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.resolveVisibleId("yok")).isInstanceOf(ResourceNotFoundException.class);
    }

    // ---- resolvePostingPortalId ----

    @Test
    void resolvePostingPortalId_openActive_returnsIdForAnyone() {
        when(portalRepository.findBySlug("a")).thenReturn(Optional.of(portal("a", PortalStatus.ACTIVE, PortalPostingPolicy.OPEN)));

        assertThat(service.resolvePostingPortalId("a", false)).isEqualTo(7L);
    }

    @Test
    void resolvePostingPortalId_archived_is400() {
        when(portalRepository.findBySlug("b")).thenReturn(Optional.of(portal("b", PortalStatus.ARCHIVED, PortalPostingPolicy.OPEN)));

        assertThatThrownBy(() -> service.resolvePostingPortalId("b", true))
                .isInstanceOf(InvalidReferenceException.class);
    }

    @Test
    void resolvePostingPortalId_hidden_is404() {
        when(portalRepository.findBySlug("h")).thenReturn(Optional.of(portal("h", PortalStatus.HIDDEN, PortalPostingPolicy.OPEN)));

        assertThatThrownBy(() -> service.resolvePostingPortalId("h", true))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void resolvePostingPortalId_staffOnly_normalUserIs403_staffAllowed() {
        when(portalRepository.findBySlug("s")).thenReturn(Optional.of(portal("s", PortalStatus.ACTIVE, PortalPostingPolicy.STAFF_ONLY)));

        assertThatThrownBy(() -> service.resolvePostingPortalId("s", false))
                .isInstanceOf(AccessDeniedException.class);
        assertThat(service.resolvePostingPortalId("s", true)).isEqualTo(7L);
    }

    @Test
    void resolvePostingPortalId_archivedIsCheckedBeforeStaffOnly() {
        when(portalRepository.findBySlug("x")).thenReturn(Optional.of(portal("x", PortalStatus.ARCHIVED, PortalPostingPolicy.STAFF_ONLY)));

        assertThatThrownBy(() -> service.resolvePostingPortalId("x", false))
                .isInstanceOf(InvalidReferenceException.class);
    }

    // ---- assertProductionBelongsToPortal ----

    @Test
    void assertProductionBelongsToPortal_nullSlug_isNoop() {
        assertThatCode(() -> service.assertProductionBelongsToPortal(7L, null)).doesNotThrowAnyException();
        verify(portalProductionRepository, never()).existsByPortalIdAndProductionSlug(any(), any());
    }

    @Test
    void assertProductionBelongsToPortal_linked_passes_unlinked_is400() {
        when(portalProductionRepository.existsByPortalIdAndProductionSlug(7L, "got")).thenReturn(true);
        when(portalProductionRepository.existsByPortalIdAndProductionSlug(7L, "st")).thenReturn(false);

        assertThatCode(() -> service.assertProductionBelongsToPortal(7L, "got")).doesNotThrowAnyException();
        assertThatThrownBy(() -> service.assertProductionBelongsToPortal(7L, "st"))
                .isInstanceOf(InvalidReferenceException.class);
    }

    // ---- getRefsByIds / i18n ----

    @Test
    void getRefsByIds_singleBatchQuery_andLocaleResolvedName() {
        Portal p = portal("westeros", PortalStatus.ACTIVE, PortalPostingPolicy.OPEN);
        when(portalRepository.findAllById(Set.of(7L))).thenReturn(List.of(p));

        LocaleContextHolder.setLocale(Locale.forLanguageTag("tr"));
        Map<Long, PortalRefResponse> tr = service.getRefsByIds(List.of(7L, 7L));
        LocaleContextHolder.setLocale(Locale.ENGLISH);
        Map<Long, PortalRefResponse> en = service.getRefsByIds(List.of(7L));

        assertThat(tr.get(7L)).isEqualTo(new PortalRefResponse("westeros", "Türkçe Ad"));
        assertThat(en.get(7L)).isEqualTo(new PortalRefResponse("westeros", "English Name"));
    }

    @Test
    void getRefsByIds_emptyInput_doesNotQuery() {
        assertThat(service.getRefsByIds(List.of())).isEmpty();
        verify(portalRepository, never()).findAllById(any());
    }

    @Test
    void getHiddenPortalIds_readsHiddenStatusOnly() {
        when(portalRepository.findIdsByStatus(PortalStatus.HIDDEN)).thenReturn(List.of(3L, 4L));

        assertThat(service.getHiddenPortalIds()).containsExactlyInAnyOrder(3L, 4L);
    }

    // ---- counters ----

    @Test
    void threadCountCounters_delegateToAtomicRepositoryUpdates() {
        service.incrementThreadCount(7L);
        service.decrementThreadCount(7L);

        verify(portalRepository).incrementThreadCount(7L);
        verify(portalRepository).decrementThreadCount(7L);
    }

    // ---- B3: taşıma hedefi / üyelik kümesi / sayaç taşıma ----

    @Test
    void resolveMoveTargetId_active_ok_archived400_hidden404() {
        when(portalRepository.findBySlug("a")).thenReturn(Optional.of(portal("a", PortalStatus.ACTIVE, PortalPostingPolicy.STAFF_ONLY)));
        when(portalRepository.findBySlug("b")).thenReturn(Optional.of(portal("b", PortalStatus.ARCHIVED, PortalPostingPolicy.OPEN)));
        when(portalRepository.findBySlug("h")).thenReturn(Optional.of(portal("h", PortalStatus.HIDDEN, PortalPostingPolicy.OPEN)));

        assertThat(service.resolveMoveTargetId("a")).isEqualTo(7L);            // STAFF_ONLY hedefe taşımak serbest
        assertThatThrownBy(() -> service.resolveMoveTargetId("b")).isInstanceOf(InvalidReferenceException.class);
        assertThatThrownBy(() -> service.resolveMoveTargetId("h")).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getJoinedVisiblePortalIds_delegatesToVisibleMembershipQuery() {
        when(portalMembershipRepository.findVisiblePortalIdsByUserId(5L)).thenReturn(Set.of(1L, 2L));

        assertThat(service.getJoinedVisiblePortalIds(5L)).containsExactlyInAnyOrder(1L, 2L);
    }

    @Test
    void moveThreadCount_decrementsOldIncrementsNew_inStableIdOrder_sameIdIsNoop() {
        var order = org.mockito.Mockito.inOrder(portalRepository);
        service.moveThreadCount(2L, 9L);
        order.verify(portalRepository).decrementThreadCount(2L);
        order.verify(portalRepository).incrementThreadCount(9L);

        var order2 = org.mockito.Mockito.inOrder(portalRepository);
        service.moveThreadCount(9L, 2L);
        order2.verify(portalRepository).incrementThreadCount(2L);   // küçük id önce
        order2.verify(portalRepository).decrementThreadCount(9L);

        org.mockito.Mockito.clearInvocations(portalRepository);
        service.moveThreadCount(4L, 4L);
        verify(portalRepository, never()).decrementThreadCount(any());
        verify(portalRepository, never()).incrementThreadCount(any());
    }

    @Test
    void getRefsByIds_neverReturnsARefForHiddenPortal() {
        Portal visible = portal("gorunur", PortalStatus.ACTIVE, PortalPostingPolicy.OPEN);
        Portal hidden = Portal.builder().id(8L).slug("gizli").nameTr("G").nameEn("G").status(PortalStatus.HIDDEN).build();
        when(portalRepository.findAllById(Set.of(7L, 8L))).thenReturn(List.of(visible, hidden));

        Map<Long, PortalRefResponse> refs = service.getRefsByIds(List.of(7L, 8L));

        assertThat(refs).containsOnlyKeys(7L);
    }
}
