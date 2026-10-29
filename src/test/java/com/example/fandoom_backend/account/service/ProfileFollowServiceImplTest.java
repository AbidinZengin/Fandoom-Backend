package com.example.fandoom_backend.account.service;

import com.example.fandoom_backend.account.dto.FollowStateResponse;
import com.example.fandoom_backend.account.entity.FollowState;
import com.example.fandoom_backend.account.event.ProfileFollowEvent;
import com.example.fandoom_backend.account.entity.ProfileFollow;
import com.example.fandoom_backend.account.repository.ProfileFollowRepository;
import com.example.fandoom_backend.common.exception.InvalidReferenceException;
import com.example.fandoom_backend.common.exception.ResourceNotFoundException;
import com.example.fandoom_backend.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfileFollowServiceImplTest {

    private static final Long ME = 7L;
    private static final Long OTHER = 42L;

    @Mock
    private ProfileFollowRepository repository;
    @Mock
    private UserService userService;
    @Mock
    private ProfilePrivacyLookup privacyLookup;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @InjectMocks
    private ProfileFollowServiceImpl service;

    @Test
    void follow_self_throwsInvalidReferenceAndNeverTouchesRepository() {
        when(userService.getIdByUsername("ben")).thenReturn(ME);

        assertThatThrownBy(() -> service.follow(ME, "ben")).isInstanceOf(InvalidReferenceException.class);

        verify(repository, never()).insertIgnore(any(), any(), any(), any());
    }

    @Test
    void follow_unknownUsername_propagatesNotFound() {
        when(userService.getIdByUsername("yok")).thenThrow(new ResourceNotFoundException("yok"));

        assertThatThrownBy(() -> service.follow(ME, "yok")).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void follow_new_savesFollowingRowAndReturnsState() {
        when(userService.getIdByUsername("arya")).thenReturn(OTHER);
        when(repository.findByFollowerIdAndFollowedId(ME, OTHER))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(row(FollowState.FOLLOWING)));
        when(repository.insertIgnore(eq(ME), eq(OTHER), eq("FOLLOWING"), any())).thenReturn(1);
        when(repository.countByFollowedIdAndStatus(OTHER, FollowState.FOLLOWING)).thenReturn(1L);

        FollowStateResponse response = service.follow(ME, "arya");

        assertThat(response.following()).isTrue();
        assertThat(response.status()).isEqualTo(FollowState.FOLLOWING);
        assertThat(response.followerCount()).isEqualTo(1L);
        verify(eventPublisher).publishEvent(
                new ProfileFollowEvent(ProfileFollowEvent.Type.FOLLOWED, ME, OTHER));
    }

    @Test
    void follow_alreadyFollowing_isIdempotentAndDoesNotSave() {
        when(userService.getIdByUsername("arya")).thenReturn(OTHER);
        when(repository.findByFollowerIdAndFollowedId(ME, OTHER)).thenReturn(Optional.of(row(FollowState.FOLLOWING)));
        when(repository.countByFollowedIdAndStatus(OTHER, FollowState.FOLLOWING)).thenReturn(3L);

        FollowStateResponse response = service.follow(ME, "arya");

        assertThat(response.following()).isTrue();
        verify(repository, never()).insertIgnore(any(), any(), any(), any());
        verify(eventPublisher, never()).publishEvent(any(Object.class));
    }

    @Test
    void follow_concurrentDuplicate_insertIgnoreReturnsZero_noEventAndIdempotent() {
        when(userService.getIdByUsername("arya")).thenReturn(OTHER);
        when(repository.findByFollowerIdAndFollowedId(ME, OTHER))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(row(FollowState.FOLLOWING)));
        when(repository.insertIgnore(any(), any(), any(), any())).thenReturn(0);
        when(repository.countByFollowedIdAndStatus(OTHER, FollowState.FOLLOWING)).thenReturn(1L);

        FollowStateResponse response = service.follow(ME, "arya");

        assertThat(response.following()).isTrue();
        verify(eventPublisher, never()).publishEvent(any(Object.class));
    }

    @Test
    void unfollow_existing_deletesRow() {
        ProfileFollow existing = row(FollowState.FOLLOWING);
        when(userService.getIdByUsername("arya")).thenReturn(OTHER);
        when(repository.findByFollowerIdAndFollowedId(ME, OTHER))
                .thenReturn(Optional.of(existing))
                .thenReturn(Optional.empty());

        FollowStateResponse response = service.unfollow(ME, "arya");

        verify(repository).delete(existing);
        verify(eventPublisher).publishEvent(
                new ProfileFollowEvent(ProfileFollowEvent.Type.WITHDRAWN, ME, OTHER));
        assertThat(response.following()).isFalse();
        assertThat(response.status()).isEqualTo(FollowState.NONE);
    }

    @Test
    void unfollow_notFollowing_isNoOp() {
        when(userService.getIdByUsername("arya")).thenReturn(OTHER);
        when(repository.findByFollowerIdAndFollowedId(ME, OTHER)).thenReturn(Optional.empty());

        service.unfollow(ME, "arya");

        verify(repository, never()).delete(any());
        verify(eventPublisher, never()).publishEvent(any(Object.class));
    }

    @Test
    void stateOf_anonymousViewer_isNoneWithoutQuery() {
        assertThat(service.stateOf(null, OTHER)).isEqualTo(FollowState.NONE);

        verify(repository, never()).findByFollowerIdAndFollowedId(any(), any());
    }

    @Test
    void filterFollowedBy_anonymousOrEmpty_returnsEmptyWithoutQuery() {
        assertThat(service.filterFollowedBy(null, Set.of(1L))).isEmpty();
        assertThat(service.filterFollowedBy(ME, Set.of())).isEmpty();

        verify(repository, never()).findFollowedIdsAmong(any(), any(), any());
    }

    @Test
    void follow_privateTarget_createsRequestedRowNotFollowing() {
        when(userService.getIdByUsername("arya")).thenReturn(OTHER);
        when(privacyLookup.isPrivate(OTHER)).thenReturn(true);
        when(repository.findByFollowerIdAndFollowedId(ME, OTHER))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(row(FollowState.REQUESTED)));
        when(repository.insertIgnore(eq(ME), eq(OTHER), eq("REQUESTED"), any())).thenReturn(1);
        when(repository.countByFollowedIdAndStatus(OTHER, FollowState.FOLLOWING)).thenReturn(0L);

        FollowStateResponse response = service.follow(ME, "arya");

        assertThat(response.following()).isFalse();
        assertThat(response.status()).isEqualTo(FollowState.REQUESTED);
        verify(eventPublisher).publishEvent(
                new ProfileFollowEvent(ProfileFollowEvent.Type.REQUESTED, ME, OTHER));
    }

    @Test
    void follow_publicTarget_doesNotAskForApproval() {
        when(userService.getIdByUsername("arya")).thenReturn(OTHER);
        when(privacyLookup.isPrivate(OTHER)).thenReturn(false);
        when(repository.findByFollowerIdAndFollowedId(ME, OTHER)).thenReturn(Optional.empty());

        service.follow(ME, "arya");

        // Açık profil: onay istenmeden doğrudan FOLLOWING eklenir.
        verify(repository).insertIgnore(eq(ME), eq(OTHER), eq("FOLLOWING"), any());
    }

    @Test
    void acceptRequest_requested_becomesFollowing() {
        ProfileFollow pending = row(FollowState.REQUESTED);
        when(userService.getIdByUsername("arya")).thenReturn(OTHER);
        when(repository.findByFollowerIdAndFollowedId(OTHER, ME)).thenReturn(Optional.of(pending));

        service.acceptRequest(ME, "arya");

        assertThat(pending.getStatus()).isEqualTo(FollowState.FOLLOWING);
        // Bildirim istek sahibine gider: actor = profil sahibi (ME), recipient = istek sahibi (OTHER).
        verify(eventPublisher).publishEvent(
                new ProfileFollowEvent(ProfileFollowEvent.Type.ACCEPTED, ME, OTHER));
    }

    @Test
    void acceptRequest_alreadyFollowing_isIdempotent() {
        ProfileFollow existing = row(FollowState.FOLLOWING);
        when(userService.getIdByUsername("arya")).thenReturn(OTHER);
        when(repository.findByFollowerIdAndFollowedId(OTHER, ME)).thenReturn(Optional.of(existing));

        service.acceptRequest(ME, "arya");

        assertThat(existing.getStatus()).isEqualTo(FollowState.FOLLOWING);
        // Tekrar onay ikinci bildirim üretmez.
        verify(eventPublisher, never()).publishEvent(any(Object.class));
    }

    @Test
    void acceptRequest_noRequest_throwsNotFound() {
        when(userService.getIdByUsername("arya")).thenReturn(OTHER);
        when(repository.findByFollowerIdAndFollowedId(OTHER, ME)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.acceptRequest(ME, "arya")).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void rejectRequest_deletesOnlyRequestedRow() {
        ProfileFollow pending = row(FollowState.REQUESTED);
        when(userService.getIdByUsername("arya")).thenReturn(OTHER);
        when(repository.findByFollowerIdAndFollowedId(OTHER, ME)).thenReturn(Optional.of(pending));

        service.rejectRequest(ME, "arya");

        verify(repository).delete(pending);
        // Reddetme istek sahibine bildirim üretmez; yalnız bayat istek bildirimini temizleyen olay yayınlanır.
        verify(eventPublisher).publishEvent(
                new ProfileFollowEvent(ProfileFollowEvent.Type.REQUEST_REJECTED, ME, OTHER));
    }

    @Test
    void rejectRequest_onApprovedFollower_doesNotRemoveThem() {
        when(userService.getIdByUsername("arya")).thenReturn(OTHER);
        when(repository.findByFollowerIdAndFollowedId(OTHER, ME))
                .thenReturn(Optional.of(row(FollowState.FOLLOWING)));

        service.rejectRequest(ME, "arya");

        verify(repository, never()).delete(any());
    }

    @Test
    void removeFollower_deletesFollowingRowButNotPendingRequest() {
        ProfileFollow follower = row(FollowState.FOLLOWING);
        when(userService.getIdByUsername("arya")).thenReturn(OTHER);
        when(repository.findByFollowerIdAndFollowedId(OTHER, ME)).thenReturn(Optional.of(follower));

        service.removeFollower(ME, "arya");

        verify(repository).delete(follower);
    }

    @Test
    void removeFollower_pendingRequest_isLeftAlone() {
        when(userService.getIdByUsername("arya")).thenReturn(OTHER);
        when(repository.findByFollowerIdAndFollowedId(OTHER, ME))
                .thenReturn(Optional.of(row(FollowState.REQUESTED)));

        service.removeFollower(ME, "arya");

        verify(repository, never()).delete(any());
    }

    @Test
    void isRestricted_owner_neverRestricted() {
        assertThat(service.isRestricted(ME, ME)).isFalse();
    }

    @Test
    void isRestricted_publicProfile_notRestricted() {
        when(privacyLookup.isPrivate(OTHER)).thenReturn(false);

        assertThat(service.isRestricted(null, OTHER)).isFalse();
    }

    @Test
    void isRestricted_privateProfile_anonymousAndPendingAreRestrictedFollowerIsNot() {
        when(privacyLookup.isPrivate(OTHER)).thenReturn(true);
        when(repository.findByFollowerIdAndFollowedId(ME, OTHER))
                .thenReturn(Optional.of(row(FollowState.REQUESTED)))
                .thenReturn(Optional.of(row(FollowState.FOLLOWING)));

        assertThat(service.isRestricted(null, OTHER)).isTrue();
        assertThat(service.isRestricted(ME, OTHER)).isTrue();
        assertThat(service.isRestricted(ME, OTHER)).isFalse();
    }

    @Test
    void approveAllRequests_approvesEachPendingRequestAndPublishesAcceptedForEach() {
        ProfileFollow first = ProfileFollow.builder().id(1L).followerId(10L).followedId(ME)
                .status(FollowState.REQUESTED).build();
        ProfileFollow second = ProfileFollow.builder().id(2L).followerId(11L).followedId(ME)
                .status(FollowState.REQUESTED).build();
        when(repository.findByFollowedIdAndStatus(ME, FollowState.REQUESTED)).thenReturn(List.of(first, second));

        assertThat(service.approveAllRequests(ME)).isEqualTo(2);

        assertThat(first.getStatus()).isEqualTo(FollowState.FOLLOWING);
        assertThat(second.getStatus()).isEqualTo(FollowState.FOLLOWING);
        // Bildirim her istek sahibine gider: actor = profil sahibi (ME), recipient = istek sahibi.
        verify(eventPublisher).publishEvent(new ProfileFollowEvent(ProfileFollowEvent.Type.ACCEPTED, ME, 10L));
        verify(eventPublisher).publishEvent(new ProfileFollowEvent(ProfileFollowEvent.Type.ACCEPTED, ME, 11L));
    }

    @Test
    void approveAllRequests_noPendingRequests_publishesNothing() {
        when(repository.findByFollowedIdAndStatus(ME, FollowState.REQUESTED)).thenReturn(List.of());

        assertThat(service.approveAllRequests(ME)).isZero();

        verify(eventPublisher, never()).publishEvent(any(Object.class));
    }

    private ProfileFollow row(FollowState state) {
        return ProfileFollow.builder().id(1L).followerId(ME).followedId(OTHER).status(state).build();
    }
}
