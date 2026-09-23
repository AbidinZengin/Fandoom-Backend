-- movie_lifecycle_migration.sql
-- movie'ye üretim döngüsü + künye alanları ekler: tagline/tagline_tr, status, budget, box_office
-- ve yönetmen/senarist tabloları (movie_directors, movie_writers).
-- portal_migration.sql ile aynı üslup, İDEMPOTENT: tekrar çalıştırmak güvenli.
--
-- ÇALIŞTIRMA SIRASI: bu script'i yeni build'in İLK açılışından ÖNCE çalıştır.
--   - status kolonunu Hibernate yaratırsa MySQL native enum(...) üretebilir; ileride yeni değer
--     eklemek ddl-auto=update ile ALTER edilmez. Burada VARCHAR(20) olarak açılır, Hibernate dokunmaz.
--   - Entity'deki adlar birebir şunlar olmalı (aksi halde Hibernate ikinci bir indeks/FK açar):
--       FK  : fk_movie_directors_movie, fk_movie_writers_movie
--       IDX : idx_movie_directors_person, idx_movie_writers_person
-- Varsayımlar: MySQL 8.0+, InnoDB, utf8mb4.

SET NAMES utf8mb4;

-- =====================================================================
-- ADIM 1 — movie kolonları (yoksa ekle)
-- =====================================================================

DROP PROCEDURE IF EXISTS add_movie_column_if_missing;

CREATE PROCEDURE add_movie_column_if_missing(IN col_name VARCHAR(64), IN col_def VARCHAR(255))
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_schema = DATABASE() AND table_name = 'movie' AND column_name = col_name) THEN
        SET @ddl = CONCAT('ALTER TABLE movie ADD COLUMN ', col_name, ' ', col_def);
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END;

CALL add_movie_column_if_missing('tagline_tr', 'VARCHAR(500) NULL');
CALL add_movie_column_if_missing('tagline',    'VARCHAR(500) NULL');
CALL add_movie_column_if_missing('status',     'VARCHAR(20) NULL');
CALL add_movie_column_if_missing('budget',     'BIGINT NULL');
CALL add_movie_column_if_missing('box_office', 'BIGINT NULL');

DROP PROCEDURE add_movie_column_if_missing;

-- =====================================================================
-- ADIM 2 — yönetmen / senarist tabloları (movie_producers ile aynı şekil)
-- =====================================================================

CREATE TABLE IF NOT EXISTS movie_directors (
    movie_id  BIGINT NOT NULL,
    person_id BIGINT NOT NULL,
    UNIQUE KEY uk_movie_directors_movie_person (movie_id, person_id),
    KEY idx_movie_directors_person (person_id, movie_id),
    CONSTRAINT fk_movie_directors_movie FOREIGN KEY (movie_id) REFERENCES movie (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS movie_writers (
    movie_id  BIGINT NOT NULL,
    person_id BIGINT NOT NULL,
    UNIQUE KEY uk_movie_writers_movie_person (movie_id, person_id),
    KEY idx_movie_writers_person (person_id, movie_id),
    CONSTRAINT fk_movie_writers_movie FOREIGN KEY (movie_id) REFERENCES movie (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- =====================================================================
-- ADIM 3 — status backfill (yalnız boş olanlar)
-- Geçmiş/bugün tarihli → RELEASED; gelecek tarihli veya tarihsiz → ANNOUNCED
-- (servisteki MovieServiceImpl.resolveStatus ile aynı kural).
-- =====================================================================

UPDATE movie
SET status = CASE
                 WHEN release_date IS NOT NULL AND release_date <= CURDATE() THEN 'RELEASED'
                 ELSE 'ANNOUNCED'
    END
WHERE status IS NULL;
