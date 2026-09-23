-- tag_content_warning_migration.sql
-- TagType'a CONTENT_WARNING eklendi. tag.type kolonunu Hibernate MySQL native
-- enum('MOOD') olarak yaratmış olabilir; ddl-auto=update mevcut enum'a yeni değer
-- eklemez (INSERT "Data truncated" ile patlar). Kolonu VARCHAR(20)'ye çevirir —
-- zaten VARCHAR ise etkisizdir, İDEMPOTENT. Yeni build'in İLK açılışından ÖNCE çalıştır.

ALTER TABLE tag MODIFY COLUMN type VARCHAR(20) NULL;
