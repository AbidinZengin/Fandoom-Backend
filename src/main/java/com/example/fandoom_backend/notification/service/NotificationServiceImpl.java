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
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationServiceImpl implements NotificationService {

    // Takip-bırak-takip et döngüsüyle bildirim spam'ini keser.
    static final Duration DEDUPE_WINDOW = Duration.ofHours(24);

    private final NotificationRepository notificationRepository;
    private final UserService userService;
    private final UserProfileService userProfileService;
    private final ProfileFollowService profileFollowService;

    @Override
    @Transactional
    public void record(Long actorId, Long recipientId, NotificationType type) {
        if (actorId.equals(recipientId)) {
            return;
        }
        boolean recentlyNotified = notificationRepository.existsByActorIdAndRecipientIdAndTypeAndCreatedAtAfter(
                actorId, recipientId, type, LocalDateTime.now().minus(DEDUPE_WINDOW));
        if (recentlyNotified) {
            return;
        }
        notificationRepository.save(Notification.builder()
                .actorId(actorId).recipientId(recipientId).type(type).read(false).build());
    }

    @Override
    @Transactional
    public void discardUnread(Long actorId, Long recipientId, Collection<NotificationType> types) {
        notificationRepository.deleteUnread(actorId, recipientId, types);
    }

    @Override
    public PageResponse<NotificationResponse> list(Long recipientId, boolean unreadOnly, Pageable pageable) {
        Page<Notification> page = unreadOnly
                ? notificationRepository.findUnreadForRecipient(recipientId, pageable)
                : notificationRepository.findAllForRecipient(recipientId, pageable);
        return enrich(page, recipientId);
    }

    @Override
    public long unreadCount(Long recipientId) {
        return notificationRepository.countByRecipientIdAndReadFalse(recipientId);
    }

    @Override
    @Transactional
    public void markRead(Long recipientId, Long id) {
        findOwned(recipientId, id).setRead(true);
    }

    @Override
    @Transactional
    public void markAllRead(Long recipientId) {
        notificationRepository.markAllRead(recipientId);
    }

    @Override
    @Transactional
    public void delete(Long recipientId, Long id) {
        notificationRepository.delete(findOwned(recipientId, id));
    }

    private Notification findOwned(Long recipientId, Long id) {
        return notificationRepository.findByIdAndRecipientId(id, recipientId)
                .orElseThrow(() -> new ResourceNotFoundException("Bildirim bulunamadı: id=" + id));
    }

    // Sayfa başına: username, profil kartı, takip durumu için birer sorgu — N+1 yok.
    private PageResponse<NotificationResponse> enrich(Page<Notification> page, Long recipientId) {
        Set<Long> actorIds = page.getContent().stream().map(Notification::getActorId).collect(Collectors.toSet());
        Map<Long, String> usernames = userService.getUsernamesByIds(actorIds);
        Map<Long, ProfileCard> cards = userProfileService.getCardsByUserIds(actorIds);
        Map<Long, FollowState> states = profileFollowService.statesOf(recipientId, actorIds);

        List<NotificationResponse> content = page.getContent().stream()
                // Silinmiş kullanıcının bildirimi gösterilmez (username çözülemez).
                .filter(n -> usernames.containsKey(n.getActorId()))
                .map(n -> {
                    ProfileCard card = cards.get(n.getActorId());
                    return new NotificationResponse(n.getId(), n.getType(), n.isRead(), n.getCreatedAt(),
                            new NotificationResponse.ActorSummary(
                                    usernames.get(n.getActorId()),
                                    card == null ? null : card.avatarUrl(),
                                    states.getOrDefault(n.getActorId(), FollowState.NONE)));
                })
                .toList();
        return new PageResponse<>(content, page.getNumber(), page.getSize(), page.getTotalElements(),
                page.getTotalPages(), page.isLast());
    }
}
