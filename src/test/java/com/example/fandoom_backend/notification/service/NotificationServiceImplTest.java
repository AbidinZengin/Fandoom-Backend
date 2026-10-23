package com.example.fandoom_backend.notification.service;

import com.example.fandoom_backend.account.entity.FollowState;
import com.example.fandoom_backend.account.service.ProfileFollowService;
import com.example.fandoom_backend.common.dto.PageResponse;
import com.example.fandoom_backend.common.exception.ResourceNotFoundException;
import com.example.fandoom_backend.community.dto.ProfileCard;
import com.example.fandoom_backend.community.service.UserProfileService;
import com.example.fandoom_backend.notification.dto.NotificationResponse;
import com.example.fandoom_backend.notification.entity.Notification;
import com.example.fandoom_backend.notification.entity.NotificationType;
import com.example.fandoom_backend.notification.repository.NotificationRepository;
import com.example.fandoom_backend.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    private static final Long ME = 7L;
    private static final Long ARYA = 42L;

    @Mock
    private NotificationRepository repository;
    @Mock
    private UserService userService;
    @Mock
    private UserProfileService userProfileService;
    @Mock
    private ProfileFollowService profileFollowService;
    @InjectMocks
    private NotificationServiceImpl service;

    @Test
    void record_toSelf_isIgnored() {
        service.record(ME, ME, NotificationType.FOLLOW);

        verify(repository, never()).save(any());
    }

    @Test
    void record_recentDuplicate_isSuppressed() {
        when(repository.existsByActorIdAndRecipientIdAndTypeAndCreatedAtAfter(
                any(), any(), any(), any())).thenReturn(true);

        service.record(ARYA, ME, NotificationType.FOLLOW);

        verify(repository, never()).save(any());
    }

    @Test
    void record_new_savesUnreadNotificationAndChecksLast24Hours() {
        when(repository.existsByActorIdAndRecipientIdAndTypeAndCreatedAtAfter(
                any(), any(), any(), any())).thenReturn(false);
        LocalDateTime before = LocalDateTime.now();

        service.record(ARYA, ME, NotificationType.FOLLOW_REQUEST);

        ArgumentCaptor<Notification> saved = ArgumentCaptor.forClass(Notification.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getActorId()).isEqualTo(ARYA);
        assertThat(saved.getValue().getRecipientId()).isEqualTo(ME);
        assertThat(saved.getValue().getType()).isEqualTo(NotificationType.FOLLOW_REQUEST);
        assertThat(saved.getValue().isRead()).isFalse();

        ArgumentCaptor<LocalDateTime> since = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(repository).existsByActorIdAndRecipientIdAndTypeAndCreatedAtAfter(
                any(), any(), any(), since.capture());
        assertThat(since.getValue()).isBefore(before.minusHours(23)).isAfter(before.minusHours(25));
    }

    @Test
    void discardUnread_delegatesToRepository() {
        List<NotificationType> types = List.of(NotificationType.FOLLOW, NotificationType.FOLLOW_REQUEST);

        service.discardUnread(ARYA, ME, types);

        verify(repository).deleteUnread(ARYA, ME, types);
    }

    @Test
    void list_enrichesActorWithUsernameAvatarAndRecipientsFollowStatus_skipsDeletedUsers() {
        Pageable pageable = PageRequest.of(0, 20);
        Notification fromArya = notification(1L, ARYA, NotificationType.FOLLOW_REQUEST);
        Notification fromGhost = notification(2L, 99L, NotificationType.FOLLOW);
        when(repository.findAllForRecipient(ME, pageable))
                .thenReturn(new PageImpl<>(List.of(fromArya, fromGhost), pageable, 2));
        when(userService.getUsernamesByIds(Set.of(ARYA, 99L))).thenReturn(Map.of(ARYA, "arya"));
        when(userProfileService.getCardsByUserIds(Set.of(ARYA, 99L)))
                .thenReturn(Map.of(ARYA, new ProfileCard("a.png", "bio", "#ffffff", false)));
        when(profileFollowService.statesOf(ME, Set.of(ARYA, 99L)))
                .thenReturn(Map.of(ARYA, FollowState.FOLLOWING));

        PageResponse<NotificationResponse> result = service.list(ME, false, pageable);

        assertThat(result.content()).hasSize(1);
        NotificationResponse first = result.content().get(0);
        assertThat(first.type()).isEqualTo(NotificationType.FOLLOW_REQUEST);
        assertThat(first.read()).isFalse();
        assertThat(first.actor().username()).isEqualTo("arya");
        assertThat(first.actor().avatarUrl()).isEqualTo("a.png");
        assertThat(first.actor().followStatus()).isEqualTo(FollowState.FOLLOWING);
    }

    @Test
    void list_actorWithoutRelationOrProfile_getsNoneAndNullAvatar() {
        Pageable pageable = PageRequest.of(0, 20);
        when(repository.findAllForRecipient(ME, pageable)).thenReturn(
                new PageImpl<>(List.of(notification(1L, ARYA, NotificationType.FOLLOW)), pageable, 1));
        when(userService.getUsernamesByIds(Set.of(ARYA))).thenReturn(Map.of(ARYA, "arya"));
        when(userProfileService.getCardsByUserIds(Set.of(ARYA))).thenReturn(Map.of());
        when(profileFollowService.statesOf(ME, Set.of(ARYA))).thenReturn(Map.of());

        NotificationResponse only = service.list(ME, false, pageable).content().get(0);

        assertThat(only.actor().avatarUrl()).isNull();
        assertThat(only.actor().followStatus()).isEqualTo(FollowState.NONE);
    }

    @Test
    void list_unreadOnly_usesUnreadQuery() {
        Pageable pageable = PageRequest.of(0, 20);
        when(repository.findUnreadForRecipient(ME, pageable)).thenReturn(new PageImpl<>(List.of(), pageable, 0));

        service.list(ME, true, pageable);

        verify(repository).findUnreadForRecipient(ME, pageable);
        verify(repository, never()).findAllForRecipient(any(), any());
    }

    @Test
    void unreadCount_delegatesToIndexedCount() {
        when(repository.countByRecipientIdAndReadFalse(ME)).thenReturn(3L);

        assertThat(service.unreadCount(ME)).isEqualTo(3L);
    }

    @Test
    void markRead_ownNotification_setsReadAndIsIdempotent() {
        Notification n = notification(1L, ARYA, NotificationType.FOLLOW);
        when(repository.findByIdAndRecipientId(1L, ME)).thenReturn(Optional.of(n));

        service.markRead(ME, 1L);
        service.markRead(ME, 1L);

        assertThat(n.isRead()).isTrue();
    }

    @Test
    void markRead_someoneElsesOrMissing_throwsNotFound() {
        when(repository.findByIdAndRecipientId(5L, ME)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.markRead(ME, 5L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void delete_someoneElsesOrMissing_throwsNotFoundAndDeletesNothing() {
        when(repository.findByIdAndRecipientId(5L, ME)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(ME, 5L)).isInstanceOf(ResourceNotFoundException.class);

        verify(repository, never()).delete(any());
    }

    @Test
    void delete_own_deletesRow() {
        Notification n = notification(1L, ARYA, NotificationType.FOLLOW);
        when(repository.findByIdAndRecipientId(1L, ME)).thenReturn(Optional.of(n));

        service.delete(ME, 1L);

        verify(repository).delete(n);
    }

    @Test
    void markAllRead_delegatesBulkUpdate() {
        service.markAllRead(ME);

        verify(repository).markAllRead(ME);
    }

    private Notification notification(Long id, Long actorId, NotificationType type) {
        Notification n = Notification.builder().id(id).recipientId(ME).actorId(actorId).type(type).build();
        n.setCreatedAt(LocalDateTime.of(2026, 10, 23, 18, 30));
        return n;
    }
}
