-- k6/db/explain_thread_queries.sql
-- Thread feed sorgularının indeks kullanımını doğrular. Önce seed_threads.sql ile veri ekleyin (boş tabloda
-- optimizer anlamlı plan seçmez). Her satırda Extra sütununda "Using filesort" OLMAMALI ve key,
-- ilgili özel indeks olmalıdır (aşağıdaki "beklenen key" yorumları).
--
-- Çalıştırma:  MYSQL_PWD=... mysql -t -h localhost -u root fandoom < k6/db/explain_thread_queries.sql
--
-- ÖNEMLİ (2026-09-19 bulgusu): tie-breaker `id ASC` olmalı. InnoDB ikincil indeksler PK'yı örtük ASC ekler;
-- `ORDER BY x DESC, id DESC` indeksi sıralamada kullandıramaz ve filesort'a düşer. Aşağıdaki son blok
-- bunu KANITLAR (DESC/DESC = filesort, DESC/ASC = filesort yok). Uygulama (ThreadServiceImpl.listByCursor,
-- KeysetSpecification) id ASC kullanır.
-- Sorgular Hibernate'in ürettiğinin sadeleştirilmiş eşdeğeridir (alias/kolon listesi farklı, plan aynı).

-- beklenen key: idx_thread_status_hot
EXPLAIN SELECT id, slug FROM thread WHERE status = 'PUBLISHED' ORDER BY hot_score DESC, id ASC LIMIT 21;
-- idx_thread_status_surface_hot
EXPLAIN SELECT id, slug FROM thread WHERE status = 'PUBLISHED' AND surface = 'THEORY' ORDER BY hot_score DESC, id ASC LIMIT 21;
-- idx_thread_status_created
EXPLAIN SELECT id, slug FROM thread WHERE status = 'PUBLISHED' ORDER BY created_at DESC, id ASC LIMIT 21;
-- idx_thread_status_surface_created
EXPLAIN SELECT id, slug FROM thread WHERE status = 'PUBLISHED' AND surface = 'THEORY' ORDER BY created_at DESC, id ASC LIMIT 21;
-- idx_thread_status_likes
EXPLAIN SELECT id, slug FROM thread WHERE status = 'PUBLISHED' ORDER BY like_count DESC, id ASC LIMIT 21;
-- idx_thread_status_surface_likes
EXPLAIN SELECT id, slug FROM thread WHERE status = 'PUBLISHED' AND surface = 'THEORY' ORDER BY like_count DESC, id ASC LIMIT 21;
-- idx_thread_status_production_hot
EXPLAIN SELECT id, slug FROM thread WHERE status = 'PUBLISHED' AND production_slug = 'k6seed-prod' ORDER BY hot_score DESC, id ASC LIMIT 21;
-- idx_thread_status_production_created
EXPLAIN SELECT id, slug FROM thread WHERE status = 'PUBLISHED' AND production_slug = 'k6seed-prod' ORDER BY created_at DESC, id ASC LIMIT 21;
-- keyset sonraki sayfa (cursor = hot_score 5.5, id 25000): idx_thread_status_hot, filesort yok
EXPLAIN SELECT id, slug FROM thread WHERE status = 'PUBLISHED'
  AND (hot_score < 5.5 OR (hot_score = 5.5 AND id > 25000)) ORDER BY hot_score DESC, id ASC LIMIT 21;

-- KANIT: tie-breaker ters yönde ise filesort (kullanmayın!)
EXPLAIN SELECT id, slug FROM thread WHERE status = 'PUBLISHED' ORDER BY hot_score DESC, id DESC LIMIT 21;
