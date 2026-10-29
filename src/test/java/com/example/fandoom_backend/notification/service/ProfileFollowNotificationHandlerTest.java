package com.example.fandoom_backend.notification.service;

import com.example.fandoom_backend.account.event.ProfileFollowMessage;
import com.example.fandoom_backend.account.event.ProfileFollowMessage.Type;
import com.example.fandoom_backend.notification.entity.NotificationType;
import com.example.fandoom_backend.notification.entity.ProcessedEvent;
import com.example.fandoom_backend.notification.repository.ProcessedEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfileFollowNotificationHandlerTest {

    private static final String GROUP = "notification-service";
    private static final String EVENT_ID = "evt-1";
    private static final Long ACTOR = 1L;
    private static final Long RECIPIENT = 2L;

    @Mock
    private ProcessedEventRepository processedEventRepository;
    @Mock
    private NotificationService notificationService;
    @InjectMocks
    private ProfileFollowNotificationHandler handler;

    private static ProfileFollowMessage message(Type type) {
        return new ProfileFollowMessage(EVENT_ID, type, ACTOR, RECIPIENT, Instant.now());
    }

    @Test
    void followed_recordsFollow() {
        handler.handle(GROUP, message(Type.FOLLOWED));

        verify(notificationService).record(ACTOR, RECIPIENT, NotificationType.FOLLOW);
    }

    @Test
    void requested_recordsFollowRequest() {
        handler.handle(GROUP, message(Type.REQUESTED));

        verify(notificationService).record(ACTOR, RECIPIENT, NotificationType.FOLLOW_REQUEST);
    }

    @Test
    void accepted_recordsFollowAcceptedForRequester() {
        handler.handle(GROUP, message(Type.ACCEPTED));

        verify(notificationService).record(ACTOR, RECIPIENT, NotificationType.FOLLOW_ACCEPTED);
    }

    @Test
    void withdrawn_discardsUnreadFollowAndFollowRequest() {
        handler.handle(GROUP, message(Type.WITHDRAWN));

        verify(notificationService).discardUnread(ACTOR, RECIPIENT,
                List.of(NotificationType.FOLLOW, NotificationType.FOLLOW_REQUEST));
    }

    @Test
    void requestRejected_producesNoNotificationOnlyClearsStaleRequestWithSwappedRoles() {
        // actor = profil sahibi (reddeden), recipient = istek sahibi: bayat bildirim istek sahibi -> profil sahibi yönünde.
        handler.handle(GROUP, message(Type.REQUEST_REJECTED));

        verify(notificationService).discardUnread(RECIPIENT, ACTOR, List.of(NotificationType.FOLLOW_REQUEST));
        verifyNoMoreInteractions(notificationService);
    }

    @Test
    void duplicateEvent_isSkipped_noNotificationAndNoSecondMarker() {
        when(processedEventRepository.existsByConsumerGroupAndEventId(GROUP, EVENT_ID)).thenReturn(true);

        handler.handle(GROUP, message(Type.FOLLOWED));

        verifyNoInteractions(notificationService);
        verify(processedEventRepository, never()).saveAndFlush(any());
    }

    @Test
    void newEvent_isMarkedProcessedBeforeNotificationIsWritten() {
        handler.handle(GROUP, message(Type.FOLLOWED));

        InOrder order = inOrder(processedEventRepository, notificationService);
        order.verify(processedEventRepository).saveAndFlush(any(ProcessedEvent.class));
        order.verify(notificationService).record(ACTOR, RECIPIENT, NotificationType.FOLLOW);
    }
}
