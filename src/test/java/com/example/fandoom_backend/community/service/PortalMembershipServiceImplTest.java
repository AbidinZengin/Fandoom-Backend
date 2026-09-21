package com.example.fandoom_backend.community.service;

import com.example.fandoom_backend.common.exception.InvalidReferenceException;
import com.example.fandoom_backend.common.exception.ResourceNotFoundException;
import com.example.fandoom_backend.community.dto.PortalMembershipStatusResponse;
import com.example.fandoom_backend.community.entity.Portal;
import com.example.fandoom_backend.community.entity.PortalStatus;
import com.example.fandoom_backend.community.repository.PortalMembershipRepository;
import com.example.fandoom_backend.community.repository.PortalRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Sayaç yalnız GERÇEKTEN yeni satırda (insertIgnore == 1) / silinen satırda (delete == 1) değişir. Eşzamanlılık
// (gerçek DB, 20 thread): PortalMembershipConcurrencyTest.
@ExtendWith(MockitoExtension.class)
class PortalMembershipServiceImplTest {

    private static final Long USER = 7L;

    @Mock private PortalRepository portalRepository;
    @Mock private PortalMembershipRepository membershipRepository;

    private PortalMembershipServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new PortalMembershipServiceImpl(portalRepository, membershipRepository);
    }

    private Portal portal(PortalStatus status, int members) {
        Portal p = Portal.builder().id(3L).slug("westeros").nameTr("W").nameEn("W").status(status).memberCount(members).build();
        when(portalRepository.findWithLockBySlug("westeros")).thenReturn(Optional.of(p));
        return p;
    }

    @Test
    void join_newMember_incrementsOnce_andReturnsCountPlusOne() {
        portal(PortalStatus.ACTIVE, 4);
        when(membershipRepository.insertIgnore(eq(3L), eq(USER), any(LocalDateTime.class))).thenReturn(1);

        PortalMembershipStatusResponse response = service.join(USER, "westeros");

        assertThat(response).isEqualTo(new PortalMembershipStatusResponse(true, 5));
        verify(portalRepository).incrementMemberCount(3L);
    }

    @Test
    void join_alreadyMember_isIdempotent_noIncrement_sameCount() {
        portal(PortalStatus.ACTIVE, 4);
        when(membershipRepository.insertIgnore(eq(3L), eq(USER), any(LocalDateTime.class))).thenReturn(0);

        PortalMembershipStatusResponse response = service.join(USER, "westeros");

        assertThat(response).isEqualTo(new PortalMembershipStatusResponse(true, 4));
        verify(portalRepository, never()).incrementMemberCount(any());
    }

    @Test
    void join_archivedPortal_newMemberIs400_existingMemberIsIdempotentOk() {
        portal(PortalStatus.ARCHIVED, 2);
        when(membershipRepository.existsByPortalIdAndUserId(3L, USER)).thenReturn(false, true);
        when(membershipRepository.insertIgnore(eq(3L), eq(USER), any(LocalDateTime.class))).thenReturn(0);

        assertThatThrownBy(() -> service.join(USER, "westeros")).isInstanceOf(InvalidReferenceException.class);
        verify(membershipRepository, never()).insertIgnore(any(), any(), any());

        assertThat(service.join(USER, "westeros")).isEqualTo(new PortalMembershipStatusResponse(true, 2));
        verify(portalRepository, never()).incrementMemberCount(any());
    }

    @Test
    void join_hiddenOrMissingPortal_is404() {
        portal(PortalStatus.HIDDEN, 0);
        when(portalRepository.findWithLockBySlug("yok")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.join(USER, "westeros")).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.join(USER, "yok")).isInstanceOf(ResourceNotFoundException.class);
        verify(membershipRepository, never()).insertIgnore(any(), any(), any());
    }

    @Test
    void leave_member_decrementsOnce_notMemberIsNoop() {
        portal(PortalStatus.ACTIVE, 4);
        when(membershipRepository.deleteMembership(3L, USER)).thenReturn(1, 0);

        assertThat(service.leave(USER, "westeros")).isEqualTo(new PortalMembershipStatusResponse(false, 3));
        verify(portalRepository).decrementMemberCount(3L);

        org.mockito.Mockito.clearInvocations(portalRepository);
        assertThat(service.leave(USER, "westeros")).isEqualTo(new PortalMembershipStatusResponse(false, 4));
        verify(portalRepository, never()).decrementMemberCount(any());
    }

    @Test
    void leave_neverReportsNegativeCount() {
        portal(PortalStatus.ACTIVE, 0);
        when(membershipRepository.deleteMembership(3L, USER)).thenReturn(1);

        assertThat(service.leave(USER, "westeros").memberCount()).isZero();
    }

    @Test
    void leave_archivedPortalIsAllowed_hiddenIs404() {
        portal(PortalStatus.ARCHIVED, 2);
        when(membershipRepository.deleteMembership(3L, USER)).thenReturn(1);

        assertThat(service.leave(USER, "westeros")).isEqualTo(new PortalMembershipStatusResponse(false, 1));
    }

    @Test
    void leave_hiddenPortal_is404() {
        portal(PortalStatus.HIDDEN, 2);

        assertThatThrownBy(() -> service.leave(USER, "westeros")).isInstanceOf(ResourceNotFoundException.class);
        verify(membershipRepository, never()).deleteMembership(any(), any());
    }
}
