package com.example.fandoom_backend.account.service;

import com.example.fandoom_backend.account.dto.FollowStateResponse;
import com.example.fandoom_backend.account.entity.FollowState;
import com.example.fandoom_backend.account.entity.ProfileFollow;
import com.example.fandoom_backend.account.repository.ProfileFollowRepository;
import com.example.fandoom_backend.common.exception.InvalidReferenceException;
import com.example.fandoom_backend.common.exception.ResourceNotFoundException;
import com.example.fandoom_backend.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
    @InjectMocks
    private ProfileFollowServiceImpl service;

    @Test
    void follow_self_throwsInvalidReferenceAndNeverTouchesRepository() {
        when(userService.getIdByUsername("ben")).thenReturn(ME);

        assertThatThrownBy(() -> service.follow(ME, "ben")).isInstanceOf(InvalidReferenceException.class);

        verify(repository, never()).saveAndFlush(any());
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
        when(repository.countByFollowedIdAndStatus(OTHER, FollowState.FOLLOWING)).thenReturn(1L);

        FollowStateResponse response = service.follow(ME, "arya");

        assertThat(response.following()).isTrue();
        assertThat(response.status()).isEqualTo(FollowState.FOLLOWING);
        assertThat(response.followerCount()).isEqualTo(1L);
        verify(repository).saveAndFlush(any(ProfileFollow.class));
    }

    @Test
    void follow_alreadyFollowing_isIdempotentAndDoesNotSave() {
        when(userService.getIdByUsername("arya")).thenReturn(OTHER);
        when(repository.findByFollowerIdAndFollowedId(ME, OTHER)).thenReturn(Optional.of(row(FollowState.FOLLOWING)));
        when(repository.countByFollowedIdAndStatus(OTHER, FollowState.FOLLOWING)).thenReturn(3L);

        FollowStateResponse response = service.follow(ME, "arya");

        assertThat(response.following()).isTrue();
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void follow_concurrentDuplicate_swallowsUniqueViolation() {
        when(userService.getIdByUsername("arya")).thenReturn(OTHER);
        when(repository.findByFollowerIdAndFollowedId(ME, OTHER))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(row(FollowState.FOLLOWING)));
        when(repository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("uk_profile_follow"));
        when(repository.countByFollowedIdAndStatus(OTHER, FollowState.FOLLOWING)).thenReturn(1L);

        FollowStateResponse response = service.follow(ME, "arya");

        assertThat(response.following()).isTrue();
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
        assertThat(response.following()).isFalse();
        assertThat(response.status()).isEqualTo(FollowState.NONE);
    }

    @Test
    void unfollow_notFollowing_isNoOp() {
        when(userService.getIdByUsername("arya")).thenReturn(OTHER);
        when(repository.findByFollowerIdAndFollowedId(ME, OTHER)).thenReturn(Optional.empty());

        service.unfollow(ME, "arya");

        verify(repository, never()).delete(any());
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

    private ProfileFollow row(FollowState state) {
        return ProfileFollow.builder().id(1L).followerId(ME).followedId(OTHER).status(state).build();
    }
}
