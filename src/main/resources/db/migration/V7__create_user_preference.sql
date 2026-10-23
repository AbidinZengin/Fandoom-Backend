-- Kullaniciya bagli genel tercih deposu (ilk anahtar: community-layout). Yalniz ekleme, mevcut tablolara dokunmaz.
-- pref_key serbest metin degil: izinli anahtarlar uygulamada PreferenceKey enum'u ile tutulur.
-- value_json VARCHAR(1024): kucuk dogrulanmis JSON (birkac yuz bayt); Hibernate validate JSON tipiyle String eslemesini kabul etmez.
CREATE TABLE IF NOT EXISTS user_preference (
    id         BIGINT        NOT NULL AUTO_INCREMENT,
    user_id    BIGINT        NOT NULL,
    pref_key   VARCHAR(64)   NOT NULL,
    value_json VARCHAR(1024) NOT NULL,
    created_at DATETIME(6)   NOT NULL,
    updated_at DATETIME(6)   NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_preference_user_key (user_id, pref_key),
    CONSTRAINT fk_user_preference_user FOREIGN KEY (user_id) REFERENCES app_user (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
