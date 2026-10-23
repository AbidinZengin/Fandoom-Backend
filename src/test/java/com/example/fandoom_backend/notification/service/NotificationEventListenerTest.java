package com.example.fandoom_backend.notification.service;

import com.example.fandoom_backend.account.event.ProfileFollowEvent;
import com.example.fandoom_backend.account.event.ProfileFollowEvent.Type;
import com.example.fandoom_backend.notification.entity.NotificationType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class NotificationEventListenerTest {

    private static final Long ACTOR = 1L;
    private static final Long RECIPIENT = 2L;

    @Mock
    private NotificationService notificationService;
    @InjectMocks
    private NotificationEventListener listener;

    @Test
    void followed_recordsFollow() {
        listener.on(new ProfileFollowEvent(Type.FOLLOWED, ACTOR, RECIPIENT));

        verify(notificationService).record(ACTOR, RECIPIENT, NotificationType.FOLLOW);
    }

    @Test
    void requested_recordsFollowRequest() {
        listener.on(new ProfileFollowEvent(Type.REQUESTED, ACTOR, RECIPIENT));

        verify(notificationService).record(ACTOR, RECIPIENT, NotificationType.FOLLOW_REQUEST);
    }

    @Test
    void accepted_recordsFollowAcceptedForRequester() {
        listener.on(new ProfileFollowEvent(Type.ACCEPTED, ACTOR, RECIPIENT));

        verify(notificationService).record(ACTOR, RECIPIENT, NotificationType.FOLLOW_ACCEPTED);
    }

    @Test
    void withdrawn_discardsUnreadFollowAndFollowRequest() {
        listener.on(new ProfileFollowEvent(Type.WITHDRAWN, ACTOR, RECIPIENT));

        verify(notificationService).discardUnread(ACTOR, RECIPIENT,
                List.of(NotificationType.FOLLOW, NotificationType.FOLLOW_REQUEST));
    }

    @Test
    void requestRejected_producesNoNotificationOnlyClearsStaleRequestWithSwappedRoles() {
        // actor = profil sahibi (reddeden), recipient = istek sahibi: bayat bildirim istek sahibi -> profil sahibi yönünde.
        listener.on(new ProfileFollowEvent(Type.REQUEST_REJECTED, ACTOR, RECIPIENT));

        verify(notificationService).discardUnread(RECIPIENT, ACTOR, List.of(NotificationType.FOLLOW_REQUEST));
        verifyNoMoreInteractions(notificationService);
    }
}
