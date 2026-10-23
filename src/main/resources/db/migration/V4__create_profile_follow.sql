-- Kişi takibi user_follow(item_type=USER) satırlarından ayrı bir tabloya taşınır:
-- takip artık bir durum (FOLLOWING/REQUESTED) taşıyor, MOVIE/SERIES/BLOG takibinde ise anlamsız.
-- MySQL DDL transaction'a girmez; yarım kalırsa yeniden çalıştırılabilsin diye her adım idempotent.

CREATE TABLE IF NOT EXISTS profile_follow (
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    created_at  DATETIME(6) NOT NULL,
    updated_at  DATETIME(6) NOT NULL,
    follower_id BIGINT      NOT NULL,
    followed_id BIGINT      NOT NULL,
    status      VARCHAR(20) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_profile_follow UNIQUE (follower_id, followed_id),
    INDEX idx_profile_follow_followed (followed_id, status, created_at),
    INDEX idx_profile_follow_follower (follower_id, status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT IGNORE INTO profile_follow (created_at, updated_at, follower_id, followed_id, status)
SELECT created_at, updated_at, user_id, item_id, 'FOLLOWING'
FROM user_follow
WHERE item_type = 'USER';

DELETE FROM user_follow WHERE item_type = 'USER';
