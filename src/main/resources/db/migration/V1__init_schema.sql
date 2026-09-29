-- =====================================================================
-- V1__init_schema.sql — BASELINE
-- Hibernate ddl-auto=update ile olusmus mevcut semanin birebir DDL karsiligi.
-- Mevcut (dolu) veritabaninda Flyway baseline-on-migrate=true +
-- baseline-version=1 ile bu dosya CALISTIRILMAZ, sadece "1 uygulandi" diye
-- isaretlenir. Bos bir veritabaninda (yeni ortam, CI, test) gercekten calisir.
-- Enum'lar bilincli olarak VARCHAR (native MySQL enum degil) — bkz. CLAUDE.md.
-- Cross-module referanslar (franchise_id, author_id, subject_id...) sadece ID'dir,
-- FOREIGN KEY YOKTUR. FK yalnizca modul-ici aggregate iliskilerinde vardir.
-- Bundan sonraki sema degisiklikleri V2__..., V3__... dosyalariyla yapilir.
-- =====================================================================

-- ---------------------------------------------------------------------
-- user / security
-- ---------------------------------------------------------------------
CREATE TABLE app_user (
    id                                  BIGINT       NOT NULL AUTO_INCREMENT,
    created_at                          DATETIME(6)  NOT NULL,
    updated_at                          DATETIME(6)  NOT NULL,
    username                            VARCHAR(50)  NOT NULL,
    email                               VARCHAR(150) NOT NULL,
    password                            VARCHAR(100) NOT NULL,
    role                                VARCHAR(20)  NOT NULL,
    premium_expires_at                  DATETIME(6)  NULL,
    email_verified                      BIT(1)       NOT NULL,
    email_verification_token            VARCHAR(36)  NULL,
    email_verification_token_expires_at DATETIME(6)  NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_user_username UNIQUE (username),
    CONSTRAINT uk_user_email UNIQUE (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE refresh_token (
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    token       VARCHAR(36) NOT NULL,
    user_id     BIGINT      NOT NULL,
    expiry_date DATETIME(6) NOT NULL,
    revoked     BIT(1)      NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_refresh_token_token UNIQUE (token),
    INDEX idx_refresh_token_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE revoked_token (
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    jti        VARCHAR(36) NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_revoked_token_jti UNIQUE (jti)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------
-- franchise / genre
-- ---------------------------------------------------------------------
CREATE TABLE franchise (
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    created_at       DATETIME(6)  NOT NULL,
    updated_at       DATETIME(6)  NOT NULL,
    name             VARCHAR(150) NOT NULL,
    slug             VARCHAR(180) NOT NULL,
    description      TEXT         NULL,
    logo_url         VARCHAR(500) NULL,
    banner_image_url VARCHAR(500) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_franchise_slug UNIQUE (slug)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE genre (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6)  NOT NULL,
    updated_at DATETIME(6)  NOT NULL,
    name_tr    VARCHAR(80)  NULL,
    name       VARCHAR(80)  NOT NULL,
    slug       VARCHAR(100) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_genre_name_tr UNIQUE (name_tr),
    CONSTRAINT uk_genre_name UNIQUE (name),
    CONSTRAINT uk_genre_slug UNIQUE (slug)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------
-- movie
-- ---------------------------------------------------------------------
CREATE TABLE movie (
    id                         BIGINT        NOT NULL AUTO_INCREMENT,
    created_at                 DATETIME(6)   NOT NULL,
    updated_at                 DATETIME(6)   NOT NULL,
    title_tr                   VARCHAR(255)  NULL,
    title                      VARCHAR(255)  NOT NULL,
    original_title             VARCHAR(255)  NULL,
    slug                       VARCHAR(280)  NOT NULL,
    synopsis_tr                TEXT          NULL,
    synopsis                   TEXT          NULL,
    tagline_tr                 VARCHAR(500)  NULL,
    tagline                    VARCHAR(500)  NULL,
    status                     VARCHAR(20)   NULL,
    budget                     BIGINT        NULL,
    box_office                 BIGINT        NULL,
    release_date               DATE          NULL,
    runtime_minutes            INT           NULL,
    poster_url                 VARCHAR(500)  NULL,
    cover_image_url            VARCHAR(500)  NULL,
    trailer_url                VARCHAR(500)  NULL,
    content_rating             VARCHAR(10)   NULL,
    origin_country             VARCHAR(2)    NULL,
    original_language          VARCHAR(2)    NULL,
    external_rating            DECIMAL(3,1)  NULL,
    external_vote_count        INT           NULL,
    external_rating_updated_at DATETIME(6)   NULL,
    imdb_id                    VARCHAR(15)   NULL,
    tmdb_id                    INT           NULL,
    franchise_id               BIGINT        NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_movie_slug UNIQUE (slug),
    CONSTRAINT uk_movie_imdb_id UNIQUE (imdb_id),
    CONSTRAINT uk_movie_tmdb_id UNIQUE (tmdb_id),
    INDEX idx_movie_franchise_id (franchise_id),
    INDEX idx_movie_release_date_id (release_date DESC, id DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE movie_genres (
    movie_id BIGINT NOT NULL,
    genre_id BIGINT NOT NULL,
    PRIMARY KEY (movie_id, genre_id),
    CONSTRAINT fk_movie_genres_movie FOREIGN KEY (movie_id) REFERENCES movie (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE movie_producers (
    movie_id  BIGINT NOT NULL,
    person_id BIGINT NOT NULL,
    PRIMARY KEY (movie_id, person_id),
    CONSTRAINT fk_movie_producers_movie FOREIGN KEY (movie_id) REFERENCES movie (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE movie_directors (
    movie_id  BIGINT NOT NULL,
    person_id BIGINT NOT NULL,
    PRIMARY KEY (movie_id, person_id),
    CONSTRAINT fk_movie_directors_movie FOREIGN KEY (movie_id) REFERENCES movie (id),
    INDEX idx_movie_directors_person (person_id, movie_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE movie_writers (
    movie_id  BIGINT NOT NULL,
    person_id BIGINT NOT NULL,
    PRIMARY KEY (movie_id, person_id),
    CONSTRAINT fk_movie_writers_movie FOREIGN KEY (movie_id) REFERENCES movie (id),
    INDEX idx_movie_writers_person (person_id, movie_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------
-- content_block (JOINED inheritance atasi) — blog_block, home_block,
-- season_block, episode_block, series_hero_block bunun alt tablolaridir
-- ---------------------------------------------------------------------
CREATE TABLE content_block (
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    created_at  DATETIME(6) NOT NULL,
    updated_at  DATETIME(6) NOT NULL,
    order_index INT         NOT NULL,
    col         VARCHAR(20) NULL,
    grid_row    VARCHAR(20) NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------
-- series (Series -> Season -> Episode aggregate)
-- ---------------------------------------------------------------------
CREATE TABLE series (
    id                         BIGINT       NOT NULL AUTO_INCREMENT,
    created_at                 DATETIME(6)  NOT NULL,
    updated_at                 DATETIME(6)  NOT NULL,
    title_tr                   VARCHAR(255) NULL,
    title                      VARCHAR(255) NOT NULL,
    original_title             VARCHAR(255) NULL,
    slug                       VARCHAR(280) NOT NULL,
    synopsis_tr                TEXT         NULL,
    synopsis                   TEXT         NULL,
    first_air_date             DATE         NULL,
    last_air_date              DATE         NULL,
    status                     VARCHAR(20)  NOT NULL,
    poster_url                 VARCHAR(500) NULL,
    cover_image_url            VARCHAR(500) NULL,
    trailer_url                VARCHAR(500) NULL,
    content_rating             VARCHAR(10)  NULL,
    origin_country             VARCHAR(2)   NULL,
    original_language          VARCHAR(2)   NULL,
    external_rating            DECIMAL(3,1) NULL,
    external_vote_count        INT          NULL,
    external_rating_updated_at DATETIME(6)  NULL,
    imdb_id                    VARCHAR(15)  NULL,
    tmdb_id                    INT          NULL,
    franchise_id               BIGINT       NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_series_slug UNIQUE (slug),
    CONSTRAINT uk_series_imdb_id UNIQUE (imdb_id),
    CONSTRAINT uk_series_tmdb_id UNIQUE (tmdb_id),
    INDEX idx_series_franchise_id (franchise_id),
    INDEX idx_series_first_air_date_id (first_air_date DESC, id DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE series_genres (
    series_id BIGINT NOT NULL,
    genre_id  BIGINT NOT NULL,
    PRIMARY KEY (series_id, genre_id),
    CONSTRAINT fk_series_genres_series FOREIGN KEY (series_id) REFERENCES series (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE series_producers (
    series_id BIGINT NOT NULL,
    person_id BIGINT NOT NULL,
    PRIMARY KEY (series_id, person_id),
    CONSTRAINT fk_series_producers_series FOREIGN KEY (series_id) REFERENCES series (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE season (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    created_at      DATETIME(6)  NOT NULL,
    updated_at      DATETIME(6)  NOT NULL,
    season_number   INT          NOT NULL,
    title_tr        VARCHAR(255) NULL,
    title           VARCHAR(255) NULL,
    air_date        DATE         NULL,
    poster_url      VARCHAR(500) NULL,
    series_id       BIGINT       NOT NULL,
    story_kicker_tr VARCHAR(255) NULL,
    story_kicker    VARCHAR(255) NULL,
    story_title_tr  VARCHAR(255) NULL,
    story_title     VARCHAR(255) NULL,
    story_dek_tr    TEXT         NULL,
    story_dek       TEXT         NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_season_series_number UNIQUE (series_id, season_number),
    CONSTRAINT fk_season_series FOREIGN KEY (series_id) REFERENCES series (id),
    INDEX idx_season_series_id (series_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE episode (
    id                         BIGINT       NOT NULL AUTO_INCREMENT,
    created_at                 DATETIME(6)  NOT NULL,
    updated_at                 DATETIME(6)  NOT NULL,
    episode_number             INT          NOT NULL,
    title_tr                   VARCHAR(255) NULL,
    title                      VARCHAR(255) NOT NULL,
    synopsis_tr                TEXT         NULL,
    synopsis                   TEXT         NULL,
    air_date                   DATE         NULL,
    duration_minutes           INT          NULL,
    still_image_url            VARCHAR(500) NULL,
    external_rating            DECIMAL(3,1) NULL,
    external_vote_count        INT          NULL,
    external_rating_updated_at DATETIME(6)  NULL,
    imdb_id                    VARCHAR(15)  NULL,
    tmdb_id                    INT          NULL,
    season_id                  BIGINT       NOT NULL,
    story_title_tr             VARCHAR(255) NULL,
    story_title                VARCHAR(255) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_episode_season_number UNIQUE (season_id, episode_number),
    CONSTRAINT uk_episode_imdb_id UNIQUE (imdb_id),
    CONSTRAINT uk_episode_tmdb_id UNIQUE (tmdb_id),
    CONSTRAINT fk_episode_season FOREIGN KEY (season_id) REFERENCES season (id),
    INDEX idx_episode_season_id (season_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE season_block (
    id                BIGINT       NOT NULL,
    season_id         BIGINT       NOT NULL,
    scene_key         VARCHAR(100) NOT NULL,
    block_type        VARCHAR(20)  NOT NULL,
    content_tr        TEXT         NULL,
    content           TEXT         NULL,
    media_episode_ref INT          NULL,
    media_caption_tr  VARCHAR(255) NULL,
    media_caption     VARCHAR(255) NULL,
    media_credit      VARCHAR(255) NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_season_block_content_block FOREIGN KEY (id) REFERENCES content_block (id),
    CONSTRAINT fk_season_block_season FOREIGN KEY (season_id) REFERENCES season (id),
    INDEX idx_season_block_season (season_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE episode_block (
    id              BIGINT       NOT NULL,
    episode_id      BIGINT       NOT NULL,
    scene_key       VARCHAR(100) NOT NULL,
    tone            VARCHAR(20)  NULL,
    pinned          BIT(1)       NOT NULL,
    scene_kicker_tr VARCHAR(255) NULL,
    scene_kicker    VARCHAR(255) NULL,
    block_type      VARCHAR(20)  NOT NULL,
    content_tr      TEXT         NULL,
    content         TEXT         NULL,
    media_url       VARCHAR(500) NULL,
    media_alt_tr    VARCHAR(255) NULL,
    media_alt       VARCHAR(255) NULL,
    media_ratio     VARCHAR(20)  NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_episode_block_content_block FOREIGN KEY (id) REFERENCES content_block (id),
    CONSTRAINT fk_episode_block_episode FOREIGN KEY (episode_id) REFERENCES episode (id),
    INDEX idx_episode_block_episode (episode_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE series_hero_block (
    id               BIGINT       NOT NULL,
    series_id        BIGINT       NOT NULL,
    block_type       VARCHAR(20)  NOT NULL,
    x                DOUBLE       NULL,
    y                DOUBLE       NULL,
    width            DOUBLE       NULL,
    height           DOUBLE       NULL,
    image_url        VARCHAR(500) NULL,
    blur_amount      INT          NULL,
    text_tr          TEXT         NULL,
    `text`           TEXT         NULL,
    background_color VARCHAR(20)  NULL,
    border_radius    VARCHAR(10)  NULL,
    font_family      VARCHAR(20)  NULL,
    font_scale       DOUBLE       NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_series_hero_block_content_block FOREIGN KEY (id) REFERENCES content_block (id),
    CONSTRAINT fk_series_hero_block_series FOREIGN KEY (series_id) REFERENCES series (id),
    INDEX idx_series_hero_block_series (series_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------
-- person (Person + Character + Cast)
-- ---------------------------------------------------------------------
CREATE TABLE person (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6)  NOT NULL,
    updated_at DATETIME(6)  NOT NULL,
    name       VARCHAR(150) NOT NULL,
    slug       VARCHAR(180) NOT NULL,
    bio_tr     TEXT         NULL,
    bio        TEXT         NULL,
    photo_url  VARCHAR(500) NULL,
    birth_date DATE         NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_person_slug UNIQUE (slug)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE characters (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    created_at     DATETIME(6)  NOT NULL,
    updated_at     DATETIME(6)  NOT NULL,
    name           VARCHAR(150) NOT NULL,
    slug           VARCHAR(180) NOT NULL,
    description_tr TEXT         NULL,
    description    TEXT         NULL,
    quote          VARCHAR(500) NULL,
    image_url      VARCHAR(500) NULL,
    subject_type   VARCHAR(20)  NOT NULL,
    subject_id     BIGINT       NOT NULL,
    billing_order  INT          NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_character_slug UNIQUE (slug),
    INDEX idx_character_subject_billing (subject_type, subject_id, billing_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE cast_member (
    id            BIGINT      NOT NULL AUTO_INCREMENT,
    created_at    DATETIME(6) NOT NULL,
    updated_at    DATETIME(6) NOT NULL,
    person_id     BIGINT      NOT NULL,
    character_id  BIGINT      NOT NULL,
    billing_order INT         NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_cast_person_character UNIQUE (person_id, character_id),
    CONSTRAINT fk_cast_person FOREIGN KEY (person_id) REFERENCES person (id),
    CONSTRAINT fk_cast_character FOREIGN KEY (character_id) REFERENCES characters (id),
    INDEX idx_cast_person_id (person_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------
-- blog
-- ---------------------------------------------------------------------
CREATE TABLE blog (
    id                             BIGINT       NOT NULL AUTO_INCREMENT,
    created_at                     DATETIME(6)  NOT NULL,
    updated_at                     DATETIME(6)  NOT NULL,
    title_tr                       VARCHAR(255) NULL,
    title                          VARCHAR(255) NOT NULL,
    slug                           VARCHAR(280) NOT NULL,
    kicker_tr                      VARCHAR(255) NULL,
    kicker                         VARCHAR(255) NULL,
    axis_tr                        VARCHAR(255) NULL,
    axis                           VARCHAR(255) NULL,
    image_url                      VARCHAR(500) NULL,
    image_url_large                VARCHAR(500) NULL,
    image_alt_tr                   VARCHAR(255) NULL,
    image_alt                      VARCHAR(255) NULL,
    spoiler_through_season_number  INT          NULL,
    spoiler_through_episode_number INT          NULL,
    recommended_rank               INT          NULL,
    spoiler_free                   BIT(1)       NOT NULL,
    status                         VARCHAR(20)  NOT NULL,
    format                         VARCHAR(20)  NULL,
    published_at                   DATETIME(6)  NULL,
    view_count                     BIGINT       NOT NULL,
    reading_time_minutes           INT          NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_blog_slug UNIQUE (slug),
    INDEX idx_blog_status_published (status, published_at DESC),
    INDEX idx_blog_status_format_published (status, format, published_at DESC),
    INDEX idx_blog_status_views (status, view_count DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE blog_block (
    id           BIGINT       NOT NULL,
    blog_id      BIGINT       NOT NULL,
    scene_key    VARCHAR(100) NOT NULL,
    block_type   VARCHAR(20)  NOT NULL,
    content_tr   TEXT         NULL,
    content      TEXT         NULL,
    image_url    VARCHAR(500) NULL,
    image_alt_tr VARCHAR(255) NULL,
    image_alt    VARCHAR(255) NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_blog_block_content_block FOREIGN KEY (id) REFERENCES content_block (id),
    CONSTRAINT fk_blog_block_blog FOREIGN KEY (blog_id) REFERENCES blog (id),
    INDEX idx_blog_block_blog (blog_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE blog_tag (
    id             BIGINT      NOT NULL AUTO_INCREMENT,
    created_at     DATETIME(6) NOT NULL,
    updated_at     DATETIME(6) NOT NULL,
    blog_id        BIGINT      NOT NULL,
    subject_type   VARCHAR(20) NULL,
    subject_id     BIGINT      NULL,
    season_number  INT         NULL,
    episode_number INT         NULL,
    franchise_id   BIGINT      NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_blog_tag_blog FOREIGN KEY (blog_id) REFERENCES blog (id),
    INDEX idx_blog_tag_blog (blog_id),
    INDEX idx_blog_tag_subject (subject_type, subject_id),
    INDEX idx_blog_tag_franchise (franchise_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE blog_relation (
    id              BIGINT      NOT NULL AUTO_INCREMENT,
    created_at      DATETIME(6) NOT NULL,
    updated_at      DATETIME(6) NOT NULL,
    source_blog_id  BIGINT      NOT NULL,
    related_blog_id BIGINT      NOT NULL,
    order_index     INT         NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_blog_relation_pair UNIQUE (source_blog_id, related_blog_id),
    CONSTRAINT fk_blog_relation_source FOREIGN KEY (source_blog_id) REFERENCES blog (id),
    CONSTRAINT fk_blog_relation_related FOREIGN KEY (related_blog_id) REFERENCES blog (id),
    INDEX idx_blog_relation_source_order (source_blog_id, order_index)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------
-- cms
-- ---------------------------------------------------------------------
CREATE TABLE home_block (
    id            BIGINT        NOT NULL,
    page          VARCHAR(40)   NOT NULL,
    entity_id     BIGINT        NULL,
    section       VARCHAR(40)   NOT NULL,
    content_type  VARCHAR(20)   NOT NULL,
    content_value VARCHAR(2000) NOT NULL,
    link_url      VARCHAR(2000) NULL,
    alt_text      VARCHAR(255)  NULL,
    is_active     BIT(1)        NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_home_block_content_block FOREIGN KEY (id) REFERENCES content_block (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------
-- tag (genel amacli etiketleme)
-- ---------------------------------------------------------------------
CREATE TABLE tag (
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    created_at  DATETIME(6) NOT NULL,
    updated_at  DATETIME(6) NOT NULL,
    name        VARCHAR(60) NOT NULL,
    slug        VARCHAR(80) NOT NULL,
    description TEXT        NULL,
    type        VARCHAR(20) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_tag_name UNIQUE (name),
    CONSTRAINT uk_tag_slug UNIQUE (slug)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE tag_assignment (
    id            BIGINT      NOT NULL AUTO_INCREMENT,
    created_at    DATETIME(6) NOT NULL,
    updated_at    DATETIME(6) NOT NULL,
    tag_id        BIGINT      NOT NULL,
    taggable_type VARCHAR(20) NOT NULL,
    taggable_id   BIGINT      NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_tag_assignment UNIQUE (tag_id, taggable_type, taggable_id),
    CONSTRAINT fk_tag_assignment_tag FOREIGN KEY (tag_id) REFERENCES tag (id),
    INDEX idx_tag_assignment_target (taggable_type, taggable_id),
    INDEX idx_tag_assignment_tag (tag_id, taggable_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------
-- trivia
-- ---------------------------------------------------------------------
CREATE TABLE trivia (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6)  NOT NULL,
    updated_at DATETIME(6)  NOT NULL,
    item_id    BIGINT       NOT NULL,
    item_type  VARCHAR(20)  NOT NULL,
    title      VARCHAR(255) NULL,
    title_tr   VARCHAR(255) NULL,
    content    TEXT         NOT NULL,
    content_tr TEXT         NULL,
    image_url  VARCHAR(500) NULL,
    is_spoiler BIT(1)       NOT NULL,
    source_url VARCHAR(500) NULL,
    created_by BIGINT       NULL,
    PRIMARY KEY (id),
    INDEX idx_trivia_item (item_id, item_type, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Kapali kume: trivia etiketleri yalniz bu tablodaki kodlardan biri olabilir.
CREATE TABLE trivia_tag (
    id       BIGINT      NOT NULL AUTO_INCREMENT,
    code     VARCHAR(30) NOT NULL,
    label_en VARCHAR(60) NOT NULL,
    label_tr VARCHAR(60) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_trivia_tag_code UNIQUE (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE trivia_tags (
    trivia_id BIGINT      NOT NULL,
    tag       VARCHAR(30) NOT NULL,
    position  INT         NOT NULL,
    PRIMARY KEY (trivia_id, position),
    CONSTRAINT fk_trivia_tags_trivia FOREIGN KEY (trivia_id) REFERENCES trivia (id),
    CONSTRAINT fk_trivia_tags_tag FOREIGN KEY (tag) REFERENCES trivia_tag (code) ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- trivia_tag baslangic verisi (trivia_tag_table_migration.sql ile ayni)
INSERT INTO trivia_tag (code, label_en, label_tr) VALUES
    ('BEHIND_THE_SCENES', 'Behind the Scenes', 'Kamera Arkası'),
    ('EASTER_EGG',        'Easter Egg',        'Easter Egg'),
    ('CASTING',           'Casting',           'Oyuncu Seçimi'),
    ('GOOF',              'Goof',              'Hata'),
    ('LORE',              'Lore',              'Lore'),
    ('Costume',           'Costume',           'Kostüm'),
    ('Music',             'Music',             'Müzik'),
    ('Reception',         'Reception',         'Yankı'),
    ('Legacy',            'Legacy',            'Miras'),
    ('Cinematography',    'Cinematography',    'Görüntü Yönetimi');

-- ---------------------------------------------------------------------
-- lore (taksonomi + lokasyon + event)
-- ---------------------------------------------------------------------
CREATE TABLE taxonomy_category (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    created_at   DATETIME(6)  NOT NULL,
    updated_at   DATETIME(6)  NOT NULL,
    name         VARCHAR(150) NOT NULL,
    slug         VARCHAR(180) NOT NULL,
    subject_type VARCHAR(20)  NOT NULL,
    subject_id   BIGINT       NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_taxonomy_category_slug UNIQUE (slug),
    INDEX idx_taxonomy_category_subject (subject_type, subject_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE content_group (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    created_at    DATETIME(6)  NOT NULL,
    updated_at    DATETIME(6)  NOT NULL,
    name          VARCHAR(150) NOT NULL,
    slug          VARCHAR(180) NOT NULL,
    image_url     VARCHAR(500) NULL,
    category_id   BIGINT       NOT NULL,
    custom_fields JSON         NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_group_slug UNIQUE (slug),
    CONSTRAINT fk_group_category FOREIGN KEY (category_id) REFERENCES taxonomy_category (id),
    INDEX idx_group_category (category_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE group_assignment (
    id            BIGINT      NOT NULL AUTO_INCREMENT,
    created_at    DATETIME(6) NOT NULL,
    updated_at    DATETIME(6) NOT NULL,
    group_id      BIGINT      NOT NULL,
    taggable_type VARCHAR(20) NOT NULL,
    taggable_id   BIGINT      NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_group_assignment UNIQUE (group_id, taggable_type, taggable_id),
    CONSTRAINT fk_group_assignment_group FOREIGN KEY (group_id) REFERENCES content_group (id),
    INDEX idx_group_assignment_target (taggable_type, taggable_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE location (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    created_at    DATETIME(6)  NOT NULL,
    updated_at    DATETIME(6)  NOT NULL,
    name          VARCHAR(150) NOT NULL,
    slug          VARCHAR(180) NOT NULL,
    image_url     VARCHAR(500) NULL,
    description   TEXT         NULL,
    custom_fields JSON         NULL,
    subject_type  VARCHAR(20)  NOT NULL,
    subject_id    BIGINT       NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_location_slug UNIQUE (slug),
    INDEX idx_location_subject (subject_type, subject_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE event (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    created_at    DATETIME(6)  NOT NULL,
    updated_at    DATETIME(6)  NOT NULL,
    name          VARCHAR(150) NOT NULL,
    description   TEXT         NULL,
    order_index   INT          NOT NULL,
    image_url     VARCHAR(500) NULL,
    pinned        BIT(1)       NOT NULL,
    custom_fields JSON         NULL,
    subject_type  VARCHAR(20)  NOT NULL,
    subject_id    BIGINT       NOT NULL,
    location_id   BIGINT       NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_event_location FOREIGN KEY (location_id) REFERENCES location (id),
    INDEX idx_event_subject_order (subject_type, subject_id, order_index),
    INDEX idx_event_location (location_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE event_participant (
    id               BIGINT      NOT NULL AUTO_INCREMENT,
    created_at       DATETIME(6) NOT NULL,
    updated_at       DATETIME(6) NOT NULL,
    event_id         BIGINT      NOT NULL,
    participant_type VARCHAR(20) NOT NULL,
    participant_id   BIGINT      NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_event_participant UNIQUE (event_id, participant_type, participant_id),
    CONSTRAINT fk_event_participant_event FOREIGN KEY (event_id) REFERENCES event (id),
    INDEX idx_event_participant_target (participant_type, participant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------
-- account (kullanici listeleri / begeni / takip / kaydet / aktivite)
-- ---------------------------------------------------------------------
CREATE TABLE user_list (
    id              BIGINT        NOT NULL AUTO_INCREMENT,
    created_at      DATETIME(6)   NOT NULL,
    updated_at      DATETIME(6)   NOT NULL,
    user_id         BIGINT        NOT NULL,
    title           VARCHAR(150)  NOT NULL,
    description     VARCHAR(2000) NULL,
    cover_image_url VARCHAR(500)  NULL,
    is_public       BIT(1)        NOT NULL,
    is_pinned       BIT(1)        NOT NULL,
    list_type       VARCHAR(20)   NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_user_list_user_id (user_id),
    INDEX idx_user_list_user_id_list_type (user_id, list_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE user_saved_item (
    id                  BIGINT       NOT NULL AUTO_INCREMENT,
    created_at          DATETIME(6)  NOT NULL,
    updated_at          DATETIME(6)  NOT NULL,
    user_id             BIGINT       NOT NULL,
    item_id             BIGINT       NOT NULL,
    item_type           VARCHAR(20)  NOT NULL,
    user_list_id        BIGINT       NOT NULL,
    progress_percentage INT          NULL,
    notes               VARCHAR(500) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_user_saved_item UNIQUE (user_id, item_id, item_type, user_list_id),
    CONSTRAINT fk_user_saved_item_user_list FOREIGN KEY (user_list_id) REFERENCES user_list (id),
    INDEX idx_user_saved_item_user_list_id (user_list_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE user_like (
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    user_id    BIGINT      NOT NULL,
    item_id    BIGINT      NOT NULL,
    item_type  VARCHAR(20) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_user_like UNIQUE (user_id, item_id, item_type),
    INDEX idx_user_like_item (item_type, item_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE user_follow (
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    user_id    BIGINT      NOT NULL,
    item_id    BIGINT      NOT NULL,
    item_type  VARCHAR(20) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_user_follow UNIQUE (user_id, item_id, item_type),
    INDEX idx_user_follow_item (item_type, item_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE user_bookmark (
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    user_id    BIGINT      NOT NULL,
    item_id    BIGINT      NOT NULL,
    item_type  VARCHAR(20) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_user_bookmark UNIQUE (user_id, item_id, item_type),
    INDEX idx_user_bookmark_item (item_type, item_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Append-only: Auditable'i extend etmez, updated_at YOK.
CREATE TABLE user_activity_log (
    id            BIGINT      NOT NULL AUTO_INCREMENT,
    user_id       BIGINT      NOT NULL,
    activity_type VARCHAR(30) NOT NULL,
    item_id       BIGINT      NULL,
    item_type     VARCHAR(20) NULL,
    created_at    DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_user_activity_log_user_id (user_id, created_at DESC),
    INDEX idx_user_activity_log_user_id_type (user_id, activity_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------
-- community (Portal, Thread, Comment ve etkilesimler)
-- portal tablolari portal_migration.sql ile birebir (CHECK, CASCADE FK, DEFAULT dahil)
-- ---------------------------------------------------------------------
CREATE TABLE portal (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    created_at     DATETIME(6)  NOT NULL,
    updated_at     DATETIME(6)  NOT NULL,
    slug           VARCHAR(50)  NOT NULL,
    name_tr        VARCHAR(60)  NOT NULL,
    name_en        VARCHAR(60)  NOT NULL,
    description_tr VARCHAR(300) NULL,
    description_en VARCHAR(300) NULL,
    banner_url     VARCHAR(500) NULL,
    icon_url       VARCHAR(500) NULL,
    accent_color   VARCHAR(7)   NULL,
    status         VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    posting_policy VARCHAR(20)  NOT NULL DEFAULT 'OPEN',
    sort_order     INT          NOT NULL DEFAULT 0,
    member_count   INT          NOT NULL DEFAULT 0,
    thread_count   INT          NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_portal_slug UNIQUE (slug),
    CONSTRAINT chk_portal_member_count CHECK (member_count >= 0),
    CONSTRAINT chk_portal_thread_count CHECK (thread_count >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Entity Auditable degil; created_at/updated_at DEFAULT'lu (INSERT kirilmasin).
CREATE TABLE portal_production (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    portal_id       BIGINT       NOT NULL,
    production_type VARCHAR(20)  NOT NULL,
    production_slug VARCHAR(280) NOT NULL,
    created_at      DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at      DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_portal_production_slug UNIQUE (production_slug),
    CONSTRAINT uk_portal_production_portal_slug UNIQUE (portal_id, production_slug),
    CONSTRAINT fk_portal_production_portal FOREIGN KEY (portal_id) REFERENCES portal (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE portal_membership (
    id        BIGINT      NOT NULL AUTO_INCREMENT,
    portal_id BIGINT      NOT NULL,
    user_id   BIGINT      NOT NULL,
    joined_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_portal_membership UNIQUE (portal_id, user_id),
    CONSTRAINT fk_portal_membership_portal FOREIGN KEY (portal_id) REFERENCES portal (id) ON DELETE CASCADE,
    INDEX idx_portal_membership_user (user_id, joined_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Portal baslangic verisi (portal_migration.sql ADIM 2 ile ayni; her thread mecburen bir portala baglidir,
-- 'genel-sohbet' varsayilan portaldir). Portal->yapim eslemesi burada YOK (yapim verisi ortama ozgu).
INSERT INTO portal (slug, name_tr, name_en, description_tr, description_en, status, posting_policy,
                    sort_order, member_count, thread_count, created_at, updated_at)
VALUES
    ('genel-sohbet',        'Genel Sohbet',        'General Chat',        'Herhangi bir yapıma bağlı olmayan serbest sohbet alanı.', 'Open chat space not tied to any production.', 'ACTIVE', 'OPEN', 0,  0, 0, NOW(6), NOW(6)),
    ('westeros',            'Westeros',            'Westeros',            'Game of Thrones ve House of the Dragon hayranlarının ortak odası.', 'Shared room for Game of Thrones and House of the Dragon fans.', 'ACTIVE', 'OPEN', 10, 0, 0, NOW(6), NOW(6)),
    ('stranger-things',     'Stranger Things',     'Stranger Things',     'Stranger Things tartışmaları, teoriler ve fan işleri.', 'Stranger Things discussions, theories and fan works.', 'ACTIVE', 'OPEN', 20, 0, 0, NOW(6), NOW(6)),
    ('severance',           'Severance',           'Severance',           'Severance tartışmaları, teoriler ve fan işleri.', 'Severance discussions, theories and fan works.', 'ACTIVE', 'OPEN', 30, 0, 0, NOW(6), NOW(6)),
    ('from',                'FROM',                'FROM',                'FROM tartışmaları, teoriler ve fan işleri.', 'FROM discussions, theories and fan works.', 'ACTIVE', 'OPEN', 40, 0, 0, NOW(6), NOW(6)),
    ('pluribus',            'Pluribus',            'Pluribus',            'Pluribus tartışmaları, teoriler ve fan işleri.', 'Pluribus discussions, theories and fan works.', 'ACTIVE', 'OPEN', 50, 0, 0, NOW(6), NOW(6)),
    ('breaking-bad',        'Breaking Bad',        'Breaking Bad',        'Breaking Bad tartışmaları, teoriler ve fan işleri.', 'Breaking Bad discussions, theories and fan works.', 'ACTIVE', 'OPEN', 60, 0, 0, NOW(6), NOW(6)),
    ('it-welcome-to-derry', 'IT: Derry''ye Hoş Geldiniz', 'IT: Welcome to Derry', 'IT: Welcome to Derry tartışmaları, teoriler ve fan işleri.', 'IT: Welcome to Derry discussions, theories and fan works.', 'ACTIVE', 'OPEN', 70, 0, 0, NOW(6), NOW(6)),
    ('the-bear',            'The Bear',            'The Bear',            'The Bear tartışmaları, teoriler ve fan işleri.', 'The Bear discussions, theories and fan works.', 'ACTIVE', 'OPEN', 80, 0, 0, NOW(6), NOW(6));

CREATE TABLE thread (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    created_at      DATETIME(6)  NOT NULL,
    updated_at      DATETIME(6)  NOT NULL,
    slug            VARCHAR(255) NOT NULL,
    surface         VARCHAR(20)  NOT NULL,
    title           VARCHAR(200) NOT NULL,
    body            TEXT         NULL,
    image_url       VARCHAR(500) NULL,
    spoiler_flagged BIT(1)       NOT NULL,
    status          VARCHAR(20)  NOT NULL,
    author_id       BIGINT       NOT NULL,
    production_slug VARCHAR(255) NULL,
    portal_id       BIGINT       NOT NULL,
    like_count      INT          NOT NULL,
    comment_count   INT          NOT NULL,
    bookmark_count  INT          NOT NULL,
    hot_score       DOUBLE       NOT NULL,
    quality_score   DOUBLE       NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_thread_slug UNIQUE (slug),
    CONSTRAINT fk_thread_portal FOREIGN KEY (portal_id) REFERENCES portal (id),
    -- surface filtreli akis: WHERE status=? AND surface=? ORDER BY <sort> DESC
    INDEX idx_thread_status_surface_hot (status, surface, hot_score DESC),
    INDEX idx_thread_status_surface_created (status, surface, created_at DESC),
    INDEX idx_thread_status_surface_likes (status, surface, like_count DESC),
    -- surface filtresiz akis (GET /api/community/feed)
    INDEX idx_thread_status_hot (status, hot_score DESC),
    INDEX idx_thread_status_created (status, created_at DESC),
    INDEX idx_thread_status_likes (status, like_count DESC),
    INDEX idx_thread_status_production_hot (status, production_slug, hot_score DESC),
    INDEX idx_thread_status_production_created (status, production_slug, created_at DESC),
    -- portal akisi
    INDEX idx_thread_portal_status_created (portal_id, status, created_at DESC),
    INDEX idx_thread_portal_status_surface_created (portal_id, status, surface, created_at DESC),
    INDEX idx_thread_portal_status_hot (portal_id, status, hot_score DESC),
    INDEX idx_thread_portal_status_surface_hot (portal_id, status, surface, hot_score DESC),
    -- profil sayaclari
    INDEX idx_thread_author_surface_status (author_id, surface, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE thread_tag (
    id        BIGINT      NOT NULL AUTO_INCREMENT,
    thread_id BIGINT      NOT NULL,
    tag       VARCHAR(50) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_thread_tag UNIQUE (thread_id, tag),
    CONSTRAINT fk_thread_tag_thread FOREIGN KEY (thread_id) REFERENCES thread (id),
    INDEX idx_thread_tag_tag (tag)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE thread_media (
    id        BIGINT       NOT NULL AUTO_INCREMENT,
    thread_id BIGINT       NOT NULL,
    type      VARCHAR(10)  NOT NULL,
    url       VARCHAR(500) NOT NULL,
    `position` INT         NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_thread_media_thread FOREIGN KEY (thread_id) REFERENCES thread (id),
    INDEX idx_thread_media_thread_position (thread_id, `position`),
    INDEX idx_thread_media_url (url)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE comment (
    id              BIGINT        NOT NULL AUTO_INCREMENT,
    created_at      DATETIME(6)   NOT NULL,
    updated_at      DATETIME(6)   NOT NULL,
    subject_type    VARCHAR(20)   NOT NULL,
    subject_id      BIGINT        NOT NULL,
    parent_id       BIGINT        NULL,
    body            VARCHAR(2000) NOT NULL,
    spoiler_flagged BIT(1)        NOT NULL,
    status          VARCHAR(20)   NOT NULL,
    author_id       BIGINT        NOT NULL,
    like_count      INT           NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_comment_parent FOREIGN KEY (parent_id) REFERENCES comment (id),
    INDEX idx_comment_subject_parent_created (subject_type, subject_id, parent_id, created_at DESC),
    INDEX idx_comment_subject_parent_likes (subject_type, subject_id, parent_id, like_count DESC),
    INDEX idx_comment_parent_created (parent_id, created_at),
    INDEX idx_comment_author_status (author_id, status),
    INDEX idx_comment_status_created_subject (status, created_at DESC, subject_type, subject_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE thread_like (
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    user_id    BIGINT      NOT NULL,
    thread_id  BIGINT      NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_thread_like UNIQUE (user_id, thread_id),
    INDEX idx_thread_like_thread (thread_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE thread_bookmark (
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    user_id    BIGINT      NOT NULL,
    thread_id  BIGINT      NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_thread_bookmark UNIQUE (user_id, thread_id),
    INDEX idx_thread_bookmark_thread (thread_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE comment_like (
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    user_id    BIGINT      NOT NULL,
    comment_id BIGINT      NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_comment_like UNIQUE (user_id, comment_id),
    INDEX idx_comment_like_comment (comment_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE tag_follow (
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    user_id    BIGINT      NOT NULL,
    tag        VARCHAR(50) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_tag_follow UNIQUE (user_id, tag),
    INDEX idx_tag_follow_tag (tag)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE user_profile (
    id                         BIGINT       NOT NULL AUTO_INCREMENT,
    created_at                 DATETIME(6)  NOT NULL,
    updated_at                 DATETIME(6)  NOT NULL,
    user_id                    BIGINT       NOT NULL,
    bio                        VARCHAR(500) NULL,
    avatar_url                 VARCHAR(500) NULL,
    banner_url                 VARCHAR(500) NULL,
    accent_color               VARCHAR(7)   NULL,
    spoiler_protection_enabled BIT(1)       NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_user_profile_user_id UNIQUE (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
