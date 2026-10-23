-- Bildirimler (ilk tur: FOLLOW, FOLLOW_REQUEST, FOLLOW_ACCEPTED). type VARCHAR: yeni tur eklemek ALTER gerektirmesin.
-- actor_id/recipient_id user/'a duz Long (FK yok, cross-module ID-only kurali).
-- is_read: READ MySQL'de ayrilmis sozcuk degil ama okunurluk/ileriye donuk guvenlik icin is_ oneki.
CREATE TABLE IF NOT EXISTS notification (
    id           BIGINT      NOT NULL AUTO_INCREMENT,
    created_at   DATETIME(6) NOT NULL,
    updated_at   DATETIME(6) NOT NULL,
    recipient_id BIGINT      NOT NULL,
    actor_id     BIGINT      NOT NULL,
    type         VARCHAR(30) NOT NULL,
    is_read      BIT(1)      NOT NULL DEFAULT b'0',
    PRIMARY KEY (id),
    -- liste (en yeni once, unreadOnly filtreli) ve okunmamis sayac
    INDEX idx_notification_recipient_read_created (recipient_id, is_read, created_at),
    INDEX idx_notification_recipient_created (recipient_id, created_at),
    -- 24 saatlik tekrar bildirim engeli ve takip birakilinca okunmamislari silme
    INDEX idx_notification_dedupe (actor_id, recipient_id, type, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
