-- =====================================================================
-- V3__create_video.sql
-- Yapima (film/dizi) bagli YouTube videolari. item_type + item_id polimorfik
-- referanstir (trivia ile ayni desen), gercek FK yoktur.
-- =====================================================================
CREATE TABLE video (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    created_at   DATETIME(6)  NOT NULL,
    updated_at   DATETIME(6)  NOT NULL,
    item_id      BIGINT       NOT NULL,
    item_type    VARCHAR(20)  NOT NULL,
    youtube_id   VARCHAR(11)  NOT NULL,
    title        VARCHAR(255) NOT NULL,
    channel_name VARCHAR(120) NOT NULL,
    channel_url  VARCHAR(500) NOT NULL,
    sort_order   INT          NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_video_item_youtube UNIQUE (item_type, item_id, youtube_id),
    INDEX idx_video_item_order (item_type, item_id, sort_order, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
