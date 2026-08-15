-- lore_migration.sql
-- group/ modülünden lore/ modülüne geçiş: GroupType enum'unu (FACTION/SPECIES)
-- admin-tanımlı TaxonomyCategory'ye dönüştürür, mevcut veriyi kaybetmeden taşır.
-- contentblock_migration.sql ile aynı desen izlenir.

-- =====================================================================
-- ADIM 1 — Uygulamayı DURDUR. Mevcut tabloları kenara al.
-- =====================================================================
RENAME TABLE content_group TO content_group_old;
RENAME TABLE group_assignment TO group_assignment_old;

-- =====================================================================
-- ADIM 2 — Uygulamayı `ddl-auto=update` ile BAŞLAT.
-- Hibernate şu tabloları sıfırdan doğru şemayla kurar:
--   taxonomy_category, content_group (yeni şema: category_id, custom_fields),
--   group_assignment (aynı şema), location, event, event_participant
-- Kurulum bittikten hemen sonra uygulamayı tekrar DURDUR, ADIM 3'e geç.
-- =====================================================================

-- =====================================================================
-- ADIM 3 — Veriyi taşı.
-- =====================================================================

-- 3a) content_group_old'daki her benzersiz (subject_type, subject_id, type)
--     kombinasyonu için bir TaxonomyCategory türet.
INSERT INTO taxonomy_category (name, slug, subject_type, subject_id, created_at, updated_at)
SELECT DISTINCT
    CASE type WHEN 'FACTION' THEN 'Faction' WHEN 'SPECIES' THEN 'Species' END,
    CONCAT(LOWER(type), '-', LOWER(subject_type), '-', subject_id),
    subject_type, subject_id, NOW(), NOW()
FROM content_group_old;

-- 3b) content_group_old -> content_group (id'ler aynen korunur, category_id
--     join ile bulunur; customFields boş başlar, eskiden hiç yoktu)
INSERT INTO content_group (id, name, slug, image_url, category_id, custom_fields, created_at, updated_at)
SELECT g.id, g.name, g.slug, g.image_url, tc.id, NULL, g.created_at, g.updated_at
FROM content_group_old g
JOIN taxonomy_category tc
  ON tc.subject_type = g.subject_type AND tc.subject_id = g.subject_id
 AND tc.slug = CONCAT(LOWER(g.type), '-', LOWER(g.subject_type), '-', g.subject_id);

-- 3c) group_assignment_old -> group_assignment (id/group_id referansları
--     aynen korunur, content_group id'leri değişmediği için çakışma yok)
INSERT INTO group_assignment (id, group_id, taggable_type, taggable_id, created_at, updated_at)
SELECT id, group_id, taggable_type, taggable_id, created_at, updated_at
FROM group_assignment_old;

-- 3d) AUTO_INCREMENT sayaçlarını taşınan max id'nin üzerine çek
SET @sql = (SELECT CONCAT('ALTER TABLE taxonomy_category AUTO_INCREMENT = ',
    (SELECT IFNULL(MAX(id), 0) + 1 FROM taxonomy_category)));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT CONCAT('ALTER TABLE content_group AUTO_INCREMENT = ',
    (SELECT IFNULL(MAX(id), 0) + 1 FROM content_group)));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT CONCAT('ALTER TABLE group_assignment AUTO_INCREMENT = ',
    (SELECT IFNULL(MAX(id), 0) + 1 FROM group_assignment)));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- =====================================================================
-- ADIM 4 — Doğrula.
-- =====================================================================
-- GET /api/lore/groups/{id}, GET /api/lore/groups/assignments?taggableType=CHARACTER&taggableId=...
-- ile eski Faction/Species verilerinin doğru category altında göründüğünü kontrol et.
-- SELECT COUNT(*) FROM content_group_old; -- eski satır sayısı
-- SELECT COUNT(*) FROM content_group;     -- yeni satır sayısı (eşit olmalı)
--
-- Sorun yoksa eski tabloları sil:
-- DROP TABLE content_group_old;
-- DROP TABLE group_assignment_old;
--
-- location, event, event_participant tabloları için taşınacak eski veri
-- YOK — Hibernate ADIM 2'de boş kurar, bu script onlara dokunmaz.
