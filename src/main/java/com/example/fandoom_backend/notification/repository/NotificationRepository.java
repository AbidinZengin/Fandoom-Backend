package com.example.fandoom_backend.notification.repository;

import com.example.fandoom_backend.notification.entity.Notification;
import com.example.fandoom_backend.notification.entity.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // En yeni önce; sıralama sorguda sabit (Pageable'ın sort'u kullanılmaz).
    @Query("select n from Notification n where n.recipientId = :recipientId order by n.createdAt desc, n.id desc")
    Page<Notification> findAllForRecipient(@Param("recipientId") Long recipientId, Pageable pageable);

    @Query("select n from Notification n where n.recipientId = :recipientId and n.read = false "
            + "order by n.createdAt desc, n.id desc")
    Page<Notification> findUnreadForRecipient(@Param("recipientId") Long recipientId, Pageable pageable);

    long countByRecipientIdAndReadFalse(Long recipientId);

    Optional<Notification> findByIdAndRecipientId(Long id, Long recipientId);

    // Spam önleme: aynı (actor, recipient, type) için eşik zamanından sonra bildirim var mı.
    boolean existsByActorIdAndRecipientIdAndTypeAndCreatedAtAfter(Long actorId, Long recipientId,
                                                                   NotificationType type, LocalDateTime after);

    @Modifying
    @Query("delete from Notification n where n.actorId = :actorId and n.recipientId = :recipientId "
            + "and n.type in :types and n.read = false")
    int deleteUnread(@Param("actorId") Long actorId, @Param("recipientId") Long recipientId,
                     @Param("types") Collection<NotificationType> types);

    @Modifying
    @Query("update Notification n set n.read = true where n.recipientId = :recipientId and n.read = false")
    int markAllRead(@Param("recipientId") Long recipientId);
}
