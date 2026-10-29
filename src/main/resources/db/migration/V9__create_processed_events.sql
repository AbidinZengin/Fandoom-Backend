-- Kafka consumer idempotency: hangi consumer grubu hangi event'i işledi. Aynı eventId ikinci kez gelirse atlanır.
-- (consumer_group, event_id) UNIQUE: aynı event'i farklı gruplar (ör. bildirim ve e-posta) bağımsız işleyebilsin;
-- aynı anda gelen iki kopyada ikincisi UNIQUE ihlaliyle reddedilir (transaction geri alınır).
-- processed_at indeksi: eski kayıtları temizleyen iş için (Kafka retention süresinden uzun tutulmalı).
CREATE TABLE IF NOT EXISTS processed_events (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    consumer_group VARCHAR(100) NOT NULL,
    event_id       VARCHAR(36)  NOT NULL,
    processed_at   DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_processed_events_group_event (consumer_group, event_id),
    INDEX idx_processed_events_processed_at (processed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
