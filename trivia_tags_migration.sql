-- trivia_tags_migration.sql
-- Trivia'nın tek `tag` kolonu (enum/varchar) yerine çoklu + serbest metin etiketler için
-- `trivia_tags` tablosu geldi. Eski `trivia.tag` kolonu NOT NULL olduğundan, silinmezse yeni
-- build'in INSERT'leri patlar (ddl-auto=update kolon silmez). Bu script:
--   1) trivia_tags tablosunu (Hibernate ile aynı şemada) yoksa oluşturur,
--   2) eski trivia.tag değerlerini position=0 olarak kopyalar,
--   3) trivia.tag kolonunu düşürür.
-- İDEMPOTENT (tag kolonu yoksa 2-3 atlanır). Yeni build'in İLK açılışından ÖNCE çalıştırılmalı;
-- açılıştan sonra çalıştırılırsa da güvenlidir (tablo zaten vardır, kopya yalnız boş olanlara yapılır).
-- Eski tablo adı production_trivia ise önce: RENAME TABLE production_trivia TO trivia;

CREATE TABLE IF NOT EXISTS trivia_tags (
    trivia_id BIGINT       NOT NULL,
    tag       VARCHAR(30)  NOT NULL,
    position  INT          NOT NULL,
    PRIMARY KEY (trivia_id, position),
    CONSTRAINT fk_trivia_tags_trivia FOREIGN KEY (trivia_id) REFERENCES trivia (id)
);

SET @has_tag := (SELECT COUNT(*) FROM information_schema.columns
                 WHERE table_schema = DATABASE() AND table_name = 'trivia' AND column_name = 'tag');

SET @copy := IF(@has_tag > 0,
    'INSERT INTO trivia_tags (trivia_id, tag, position)
       SELECT t.id, t.tag, 0 FROM trivia t
       WHERE t.tag IS NOT NULL
         AND NOT EXISTS (SELECT 1 FROM trivia_tags x WHERE x.trivia_id = t.id)',
    'SELECT 1');
PREPARE stmt FROM @copy; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @drop := IF(@has_tag > 0, 'ALTER TABLE trivia DROP COLUMN tag', 'SELECT 1');
PREPARE stmt FROM @drop; EXECUTE stmt; DEALLOCATE PREPARE stmt;
