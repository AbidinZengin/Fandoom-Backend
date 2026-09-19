-- k6/db/cleanup_seed.sql
-- k6 testlerinin bıraktığı verileri siler:
--   * seed_threads.sql'in eklediği thread'ler  (slug 'k6seed-%')
--   * create_thread_test.js'in yazdığı thread'ler (başlık '[k6]%') ve onların tag'leri
--   * (opsiyonel) manuel cache-evict denemesinden kalan 'cache-evict-test (silinecek)' satırı
--
-- Çalıştırma:  MYSQL_PWD=... mysql -h localhost -u root fandoom < k6/db/cleanup_seed.sql
-- Önce ne silineceğine bakın (SELECT), sonra DELETE'ler çalışır.

SELECT 'silinecek thread' AS ne, COUNT(*) AS adet
FROM thread
WHERE slug LIKE 'k6seed-%' OR title LIKE '[k6]%' OR title = 'cache-evict-test (silinecek)';

DELETE FROM thread_tag
WHERE thread_id IN (SELECT id FROM (
    SELECT id FROM thread
    WHERE slug LIKE 'k6seed-%' OR title LIKE '[k6]%' OR title = 'cache-evict-test (silinecek)') t);

DELETE FROM thread
WHERE slug LIKE 'k6seed-%' OR title LIKE '[k6]%' OR title = 'cache-evict-test (silinecek)';

SELECT 'kalan thread' AS ne, COUNT(*) AS adet FROM thread;
