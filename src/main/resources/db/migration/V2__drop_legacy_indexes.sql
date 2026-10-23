-- =====================================================================
-- V2__drop_legacy_indexes.sql
-- ddl-auto=update doneminde yeniden adlandirilan indeksler Hibernate tarafindan
-- DROP edilmemisti; baseline'li (eski) veritabanlarinda kalinti olarak duruyor.
-- V1 baseline'inda bunlarin yerini alan indeksler (ayni sutunla baslayan, daha
-- kapsamli) zaten var; kalintilar yazma maliyeti ve disk yuku getiriyor.
--
-- GUVENLI VE IDEMPOTENT: her indeks yalnizca (eski VAR) VE (yenisi VAR) ise
-- dusurulur. Bos DB'de (V1 eski adlari hic olusturmaz) hicbir sey yapmaz.
-- Yenisi yoksa eskiyi ASLA dusurmez (FK/sorgu icin tek kapsayan indeks olabilir).
-- MySQL'de DROP INDEX IF EXISTS yok; bu yuzden information_schema + PREPARE.
-- =====================================================================

-- thread: idx_thread_author -> idx_thread_author_surface_status
SET @sql = (SELECT IF(
    EXISTS (SELECT 1 FROM information_schema.statistics
            WHERE table_schema = DATABASE() AND table_name = 'thread' AND index_name = 'idx_thread_author')
    AND EXISTS (SELECT 1 FROM information_schema.statistics
            WHERE table_schema = DATABASE() AND table_name = 'thread' AND index_name = 'idx_thread_author_surface_status'),
    'ALTER TABLE thread DROP INDEX idx_thread_author',
    'DO 0'));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- comment: idx_comment_subject_status_created -> idx_comment_subject_parent_created
SET @sql = (SELECT IF(
    EXISTS (SELECT 1 FROM information_schema.statistics
            WHERE table_schema = DATABASE() AND table_name = 'comment' AND index_name = 'idx_comment_subject_status_created')
    AND EXISTS (SELECT 1 FROM information_schema.statistics
            WHERE table_schema = DATABASE() AND table_name = 'comment' AND index_name = 'idx_comment_subject_parent_created'),
    'ALTER TABLE comment DROP INDEX idx_comment_subject_status_created',
    'DO 0'));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- comment: idx_comment_parent -> idx_comment_parent_created
SET @sql = (SELECT IF(
    EXISTS (SELECT 1 FROM information_schema.statistics
            WHERE table_schema = DATABASE() AND table_name = 'comment' AND index_name = 'idx_comment_parent')
    AND EXISTS (SELECT 1 FROM information_schema.statistics
            WHERE table_schema = DATABASE() AND table_name = 'comment' AND index_name = 'idx_comment_parent_created'),
    'ALTER TABLE comment DROP INDEX idx_comment_parent',
    'DO 0'));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- comment: idx_comment_author -> idx_comment_author_status
SET @sql = (SELECT IF(
    EXISTS (SELECT 1 FROM information_schema.statistics
            WHERE table_schema = DATABASE() AND table_name = 'comment' AND index_name = 'idx_comment_author')
    AND EXISTS (SELECT 1 FROM information_schema.statistics
            WHERE table_schema = DATABASE() AND table_name = 'comment' AND index_name = 'idx_comment_author_status'),
    'ALTER TABLE comment DROP INDEX idx_comment_author',
    'DO 0'));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- blog_relation: idx_blog_relation_source -> idx_blog_relation_source_order
SET @sql = (SELECT IF(
    EXISTS (SELECT 1 FROM information_schema.statistics
            WHERE table_schema = DATABASE() AND table_name = 'blog_relation' AND index_name = 'idx_blog_relation_source')
    AND EXISTS (SELECT 1 FROM information_schema.statistics
            WHERE table_schema = DATABASE() AND table_name = 'blog_relation' AND index_name = 'idx_blog_relation_source_order'),
    'ALTER TABLE blog_relation DROP INDEX idx_blog_relation_source',
    'DO 0'));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- event: idx_event_subject -> idx_event_subject_order
SET @sql = (SELECT IF(
    EXISTS (SELECT 1 FROM information_schema.statistics
            WHERE table_schema = DATABASE() AND table_name = 'event' AND index_name = 'idx_event_subject')
    AND EXISTS (SELECT 1 FROM information_schema.statistics
            WHERE table_schema = DATABASE() AND table_name = 'event' AND index_name = 'idx_event_subject_order'),
    'ALTER TABLE event DROP INDEX idx_event_subject',
    'DO 0'));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- characters: idx_character_subject -> idx_character_subject_billing
SET @sql = (SELECT IF(
    EXISTS (SELECT 1 FROM information_schema.statistics
            WHERE table_schema = DATABASE() AND table_name = 'characters' AND index_name = 'idx_character_subject')
    AND EXISTS (SELECT 1 FROM information_schema.statistics
            WHERE table_schema = DATABASE() AND table_name = 'characters' AND index_name = 'idx_character_subject_billing'),
    'ALTER TABLE characters DROP INDEX idx_character_subject',
    'DO 0'));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
