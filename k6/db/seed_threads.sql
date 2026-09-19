-- k6/db/seed_threads.sql
-- Yük/EXPLAIN testleri için 50.000 SENTETİK, PUBLISHED thread ekler (dev DB'nin thread tablosuna).
-- İşaret: slug 'k6seed-<n>', başlık '[k6-seed] <n>'. Temizlik: k6/db/cleanup_seed.sql
--
-- Çalıştırma (parola komut satırına yazılmasın diye MYSQL_PWD env'i ile):
--   MYSQL_PWD=... mysql -h localhost -u root fandoom < k6/db/seed_threads.sql
--
-- Dağılım: 3 surface, %20'si 'k6seed-prod' production_slug'lı, like 0-99, yorum 0-19, son 90 güne yayılmış
-- created_at. hot_score, HotScoreJob ile aynı formülle (like + 2*yorum) / (yaşSaat + 2)^1.5 hesaplanır.
-- UYARI: prod'a karşı ÇALIŞTIRMA.

SET SESSION cte_max_recursion_depth = 100000;

INSERT INTO thread (created_at, updated_at, author_id, body, bookmark_count, comment_count, hot_score, like_count,
                    quality_score, slug, spoiler_flagged, status, surface, title, production_slug)
WITH RECURSIVE seq AS (
    SELECT 1 AS n UNION ALL SELECT n + 1 FROM seq WHERE n < 50000
)
SELECT ts, ts, 1, 'k6 seed', 0, cc,
       (lc + 2.0 * cc) / POW(TIMESTAMPDIFF(HOUR, ts, NOW(6)) + 2, 1.5),
       lc, 0,
       CONCAT('k6seed-', n), 0, 'PUBLISHED',
       ELT(1 + n % 3, 'DISCUSSION', 'THEORY', 'FAN_ART'),
       CONCAT('[k6-seed] ', n),
       IF(n % 5 = 0, 'k6seed-prod', NULL)
FROM (
    SELECT n,
           NOW(6) - INTERVAL ((n * 37) % 129600) MINUTE AS ts,
           (n * 7) % 100 AS lc,
           (n * 3) % 20 AS cc
    FROM seq
) s;

ANALYZE TABLE thread;
SELECT status, COUNT(*) AS adet FROM thread GROUP BY status;
