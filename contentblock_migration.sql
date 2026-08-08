-- ContentBlock refactor veri taşıma script'i
-- blog_block + page_content -> content_block (+ blog_block/home_block, JOINED inheritance)
--
-- SIRAYLA UYGULA:

-- =========================================================
-- ADIM 1 — UYGULAMAYI YENİDEN BAŞLATMADAN ÖNCE çalıştır.
-- Eski tabloları kenara al ki Hibernate (ddl-auto=update) onların üstüne
-- yazıp karıştırmasın; sıfırdan doğru JOINED şemasını kursun.
-- =========================================================
RENAME TABLE blog_block TO blog_block_old;
RENAME TABLE page_content TO page_content_old;

-- =========================================================
-- ADIM 2 — Bu noktada uygulamayı normal şekilde başlat.
-- Hibernate (ddl-auto=update) şu üç tabloyu SIFIRDAN, doğru şemayla kurar:
--   content_block (id, order_index, col, grid_row, created_at, updated_at)
--   blog_block    (id FK->content_block, blog_id, block_type, text, image_url, image_alt)
--   home_block    (id FK->content_block, page, entity_id, section, content_type,
--                  content_value, link_url, alt_text, is_active)
-- Başladıktan sonra UYGULAMAYI DURDUR, ADIM 3'e geç.
-- =========================================================

-- =========================================================
-- ADIM 3 — Veriyi taşı.
-- ÖNEMLİ: content_block artık TÜM alt tipler (blog_block + home_block) için
-- PAYLAŞILAN tek id alanı. blog_block_old ve page_content_old kendi ayrı
-- AUTO_INCREMENT dizilerinden geldiği için id'leri çakışabilir — bu yüzden
-- page_content_old tarafına +1000000 offset uygulanıyor (veri azsa bu kadar
-- fazlasıyla yeterli boşluk).
-- =========================================================

-- 3a) blog_block_old -> content_block + blog_block (id'ler AYNEN korunuyor)
INSERT INTO content_block (id, order_index, col, grid_row, created_at, updated_at)
SELECT id, order_index, NULL, NULL, created_at, updated_at
FROM blog_block_old;

INSERT INTO blog_block (id, blog_id, block_type, text, image_url, image_alt)
SELECT id, blog_id, block_type, text, image_url, image_alt
FROM blog_block_old;

-- 3b) page_content_old -> content_block + home_block (id'lere +1000000 offset)
INSERT INTO content_block (id, order_index, col, grid_row, created_at, updated_at)
SELECT id + 1000000, order_index, NULL, NULL, created_at, updated_at
FROM page_content_old;

INSERT INTO home_block (id, page, entity_id, section, content_type, content_value, link_url, alt_text, is_active)
SELECT id + 1000000, page, entity_id, section, content_type, content_value, link_url, alt_text, is_active
FROM page_content_old;

-- 3c) content_block'un AUTO_INCREMENT sayacını, taşınan en yüksek id'nin
--     üzerine çek — yoksa sıradaki yeni kayıt çakışan id üretebilir.
SET @max_id = (SELECT MAX(id) FROM content_block);
SET @sql = CONCAT('ALTER TABLE content_block AUTO_INCREMENT = ', @max_id + 1);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- =========================================================
-- ADIM 4 — Doğrula.
-- Uygulamayı tekrar başlat, GET /api/blogs/{slug} ve
-- GET /api/cms/pages/{pageName} ile eski verilerin göründüğünü kontrol et.
-- Sorun yoksa:
-- =========================================================
-- DROP TABLE blog_block_old;
-- DROP TABLE page_content_old;
