-- portal_migration.sql
-- community/ modülüne Portal (oda) katmanı ekler: portal, portal_production,
-- portal_membership tabloları + thread.portal_id (NOT NULL, FK, indeksler) + sayaç backfill.
-- lore_migration.sql / contentblock_migration.sql ile aynı üslup, ama İDEMPOTENT: tekrar çalıştırmak güvenli.
--
-- =====================================================================
-- ÇALIŞTIRMA SIRASI (ÖNEMLİ)
-- =====================================================================
-- 1) Uygulamayı DURDUR (sayaç backfill'i ve NOT NULL geçişi canlı yazmayla yarışmasın).
-- 2) BU SCRIPT'İ, portal entity'lerini içeren yeni build'in ilk açılışından ÖNCE çalıştır.
--    Aksi halde `ddl-auto=update` thread'e portal_id'yi (varsa NOT NULL) mevcut satırlarla ekleyip/eklemeyip
--    tutarsız bırakabilir (MySQL NOT NULL kolonu 0 ile doldurur -> FK/backfill bozulur) ve entity'deki
--    @Index/@ForeignKey adları bu dosyadakilerden farklıysa Hibernate AYNI kolonlara İKİNCİ bir indeks/FK açar.
--    => Entity'deki adlar birebir şunlar olmalı (bkz. ADIM 2/3):
--       FK  : fk_thread_portal (thread.portal_id -> portal.id)  [düz Long ise FK'yı yalnız bu script yönetir]
--       IDX : idx_thread_portal_status_created, idx_thread_portal_status_hot,
--             idx_thread_portal_status_surface_created, idx_thread_portal_status_surface_hot
--       Portal tablosu: uk_portal_slug; portal_production: uk_portal_production_slug,
--             uk_portal_production_portal_slug; portal_membership: uk_portal_membership,
--             idx_portal_membership_user
-- 3) Script'i mysql CLI veya Workbench'te tek seferde çalıştır (DELIMITER kullanılmadı, JDBC/Workbench uyumlu).
-- 4) Uygulamayı yeni build ile başlat. ddl-auto=update artık hiçbir şeyi değiştirmemeli.
--
-- Varsayımlar: MySQL 8.0.16+ (CHECK, DESC indeks, IF EXISTS-siz idempotency için information_schema),
-- InnoDB, utf8mb4. Enum alanları varchar(20) (EnumType.STRING); Hibernate update mevcut kolon tipine dokunmaz.

SET NAMES utf8mb4;

-- =====================================================================
-- ADIM 1 — Tablolar (CREATE TABLE IF NOT EXISTS)
-- =====================================================================

CREATE TABLE IF NOT EXISTS portal (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    slug           VARCHAR(50)  NOT NULL,
    name_tr        VARCHAR(60)  NOT NULL,
    name_en        VARCHAR(60)  NOT NULL,
    description_tr VARCHAR(300) NULL,
    description_en VARCHAR(300) NULL,
    banner_url     VARCHAR(500) NULL,
    icon_url       VARCHAR(500) NULL,
    accent_color   VARCHAR(7)   NULL,                         -- opsiyonel #RRGGBB (FE gradyan/okunabilirlik için); backend yalnız saklar
    status         VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',   -- ACTIVE | ARCHIVED | HIDDEN
    posting_policy VARCHAR(20)  NOT NULL DEFAULT 'OPEN',      -- OPEN | STAFF_ONLY
    sort_order     INT          NOT NULL DEFAULT 0,
    member_count   INT          NOT NULL DEFAULT 0,
    thread_count   INT          NOT NULL DEFAULT 0,
    created_at     DATETIME(6)  NOT NULL,
    updated_at     DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_portal_slug UNIQUE (slug),
    CONSTRAINT chk_portal_member_count CHECK (member_count >= 0),
    CONSTRAINT chk_portal_thread_count CHECK (thread_count >= 0)
) ENGINE = InnoDB;
-- Liste/sort için ek indeks YOK: portal tablosu küçük (onlarca-yüzlerce satır), ORDER BY member_count/name/
-- sort_order tam taramada bile ihmal edilebilir; yazma maliyeti (member_count her join/leave'te değişir) getirisinden büyük.

CREATE TABLE IF NOT EXISTS portal_production (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    portal_id       BIGINT       NOT NULL,
    production_type VARCHAR(20)  NOT NULL,                    -- MOVIE | SERIES
    production_slug VARCHAR(280) NOT NULL,                    -- movie.slug(280) / series.slug ile aynı boy; gerçek FK yok (cross-module, ID/slug-only)
    -- DEFAULT: PortalProduction Auditable'dan türemese de (entity created_at/updated_at yazmasa da) INSERT kırılmasın.
    created_at      DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at      DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    -- bir yapım EN FAZLA bir portala bağlı (thread backfill'inin de deterministik olmasını sağlar)
    CONSTRAINT uk_portal_production_slug UNIQUE (production_slug),
    -- (portal_id, production_slug): "bu yapım bu portalın mı?" doğrulaması + portal_id önekiyle portala göre listeleme
    CONSTRAINT uk_portal_production_portal_slug UNIQUE (portal_id, production_slug),
    CONSTRAINT fk_portal_production_portal FOREIGN KEY (portal_id) REFERENCES portal (id) ON DELETE CASCADE
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS portal_membership (
    id        BIGINT      NOT NULL AUTO_INCREMENT,
    portal_id BIGINT      NOT NULL,
    user_id   BIGINT      NOT NULL,                           -- user/ modülüne gerçek FK yok (cross-module ID-only)
    joined_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    -- idempotent join: yarış halinde de tek satır. "Bu kullanıcı üye mi / portalın üyeleri" için (portal_id, user_id) yeter.
    CONSTRAINT uk_portal_membership UNIQUE (portal_id, user_id),
    CONSTRAINT fk_portal_membership_portal FOREIGN KEY (portal_id) REFERENCES portal (id) ON DELETE CASCADE,
    -- GET /api/me/portals: WHERE user_id=? ORDER BY joined_at DESC (filesort'suz) + scope=joined için user_id -> portal_id çözümü
    INDEX idx_portal_membership_user (user_id, joined_at DESC)
) ENGINE = InnoDB;

-- portal tablosu daha önce (accent_color'sız) oluşturulmuşsa (eski script / Hibernate ddl-auto) kolonu ekle. İdempotent.
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.COLUMNS
               WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'portal' AND COLUMN_NAME = 'accent_color') = 0,
              'ALTER TABLE portal ADD COLUMN accent_color VARCHAR(7) NULL AFTER icon_url',
              'DO 0');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

-- =====================================================================
-- ADIM 2 — Seed (idempotent: uk_portal_slug çakışmasında dokunmaz, admin'in sonradan değiştirdiği kaydı ezmez)
-- =====================================================================
-- GEÇİCİ VARSAYILANLAR (sahibi teyit edecek): isim/açıklama/sort_order metinleri placeholder'dır.

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
    ('the-bear',            'The Bear',            'The Bear',            'The Bear tartışmaları, teoriler ve fan işleri.', 'The Bear discussions, theories and fan works.', 'ACTIVE', 'OPEN', 80, 0, 0, NOW(6), NOW(6))
ON DUPLICATE KEY UPDATE slug = slug;

-- Portal -> yapım eşlemesi. Varsayım: yapım slug'ı, portal slug'ıyla aynı (westeros hariç). DB'de bulunmayan yapım
-- SESSİZCE ATLANIR (migration patlamaz); atlananlar ADIM 2 sonundaki rapor sorgusunda listelenir.
DROP TEMPORARY TABLE IF EXISTS _portal_seed_production;
CREATE TEMPORARY TABLE _portal_seed_production (
    portal_slug     VARCHAR(50)  NOT NULL,
    production_slug VARCHAR(280) NOT NULL
);
INSERT INTO _portal_seed_production (portal_slug, production_slug) VALUES
    ('westeros',            'game-of-thrones'),
    ('westeros',            'house-of-the-dragon'),
    ('stranger-things',     'stranger-things'),
    ('severance',           'severance'),
    ('from',                'from'),
    ('pluribus',            'pluribus'),
    ('breaking-bad',        'breaking-bad'),
    ('it-welcome-to-derry', 'it-welcome-to-derry'),
    ('the-bear',            'the-bear');

-- SERIES önce; bir yapım zaten (uk_portal_production_slug) başka portala bağlıysa dokunulmaz.
-- created_at/updated_at YAZILMAZ: Hibernate tabloyu önce açtıysa bu kolonlar hiç yoktur (entity Auditable değil);
-- bizim CREATE TABLE'ımızla açıldıysa DEFAULT CURRENT_TIMESTAMP(6) zaten doldurur.
INSERT INTO portal_production (portal_id, production_type, production_slug)
SELECT p.id, 'SERIES', s.slug
FROM _portal_seed_production sp
JOIN portal p ON p.slug = sp.portal_slug
JOIN series s ON s.slug = sp.production_slug
ON DUPLICATE KEY UPDATE portal_id = portal_id;

-- MOVIE: yalnız aynı slug'lı bir series yoksa.
INSERT INTO portal_production (portal_id, production_type, production_slug)
SELECT p.id, 'MOVIE', m.slug
FROM _portal_seed_production sp
JOIN portal p ON p.slug = sp.portal_slug
JOIN movie m ON m.slug = sp.production_slug
WHERE NOT EXISTS (SELECT 1 FROM series s2 WHERE s2.slug = sp.production_slug)
ON DUPLICATE KEY UPDATE portal_id = portal_id;

-- RAPOR: DB'de bulunamadığı için atlanan seed bağları (boş dönerse hepsi bağlandı).
SELECT sp.portal_slug, sp.production_slug AS skipped_production_slug
FROM _portal_seed_production sp
LEFT JOIN portal_production pp ON pp.production_slug = sp.production_slug
WHERE pp.id IS NULL;

DROP TEMPORARY TABLE IF EXISTS _portal_seed_production;

-- =====================================================================
-- ADIM 3 — thread.portal_id: ÖNCE nullable ekle -> backfill -> NOT NULL -> FK -> indeksler
-- =====================================================================

-- 3a) Kolon (yoksa nullable ekle)
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.COLUMNS
               WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'thread' AND COLUMN_NAME = 'portal_id') = 0,
              'ALTER TABLE thread ADD COLUMN portal_id BIGINT NULL',
              'DO 0');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

-- 3a-2) Hibernate ÖNCE açıldıysa (bu script'ten önce yeni build başlatıldıysa) `ddl-auto=update` kolonu NOT NULL ve
--       mevcut satırlar için 0 ile eklemiş olabilir (FK yok). 0 geçerli bir portal id'si değildir: kolonu nullable'a
--       çevirip 0'ları NULL yap ki aşağıdaki backfill bunları doldursun. Temiz (Hibernate öncesi) durumda no-op.
SET @ddl = IF((SELECT IS_NULLABLE FROM information_schema.COLUMNS
               WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'thread' AND COLUMN_NAME = 'portal_id') = 'NO'
              AND (SELECT COUNT(*) FROM thread WHERE portal_id = 0) > 0,
              'ALTER TABLE thread MODIFY COLUMN portal_id BIGINT NULL',
              'DO 0');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;
UPDATE thread SET portal_id = NULL WHERE portal_id = 0;

-- 3b) Backfill. Kural (VARSAYIM): thread.production_slug bir portala bağlıysa o portal, değilse 'genel-sohbet'.
--     uk_portal_production_slug sayesinde eşleşme tekildir (bir yapım en fazla bir portalda). Yalnız portal_id IS NULL
--     satırlar güncellenir -> tekrar çalıştırmak güvenli. updated_at'a dokunulmaz (kullanıcı görünür "düzenlendi" olmasın).
--     Çok büyük tabloda (>~1M satır) tek UPDATE uzun kilit tutar: aynı WHERE'e `AND t.id >= X AND t.id < X+50000`
--     ekleyip pencere pencere çalıştır (HotScoreRangeUpdater ile aynı fikir).
UPDATE thread t
JOIN portal_production pp ON pp.production_slug = t.production_slug
SET t.portal_id = pp.portal_id
WHERE t.portal_id IS NULL AND t.production_slug IS NOT NULL;

UPDATE thread t
SET t.portal_id = (SELECT id FROM portal WHERE slug = 'genel-sohbet')
WHERE t.portal_id IS NULL;

-- Kontrol: 0 dönmeli. > 0 ise NOT NULL adımı zaten hata verir; nedenini bul (genel-sohbet seed'i silinmiş olabilir).
SELECT COUNT(*) AS thread_without_portal FROM thread WHERE portal_id IS NULL;

-- 3c) NOT NULL (yalnız hâlâ nullable ise). NULL satır kalmışsa MySQL strict modda hata verir ve script durur — istenen davranış.
SET @ddl = IF((SELECT IS_NULLABLE FROM information_schema.COLUMNS
               WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'thread' AND COLUMN_NAME = 'portal_id') = 'YES',
              'ALTER TABLE thread MODIFY COLUMN portal_id BIGINT NOT NULL',
              'DO 0');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

-- 3d) FK (RESTRICT: portal silme akışı yok, thread'i olan portal DB seviyesinde de silinemez)
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
               WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'thread'
                 AND CONSTRAINT_NAME = 'fk_thread_portal' AND CONSTRAINT_TYPE = 'FOREIGN KEY') = 0,
              'ALTER TABLE thread ADD CONSTRAINT fk_thread_portal FOREIGN KEY (portal_id) REFERENCES portal (id)',
              'DO 0');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

-- 3e) İNDEKSLER — gerekçe dosyanın sonundaki "İNDEKS GEREKÇESİ" bölümünde.
--     Kural: eşitlik sütunları (portal_id, status[, surface]) önce, ORDER BY sütunu sona; id tie-breaker'ı InnoDB
--     PK'yı örtük ASC ekler -> sorgu (x DESC, id ASC) (bkz. KeysetSpecification).
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.STATISTICS
               WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'thread' AND INDEX_NAME = 'idx_thread_portal_status_created') = 0,
              'ALTER TABLE thread ADD INDEX idx_thread_portal_status_created (portal_id, status, created_at DESC), ALGORITHM=INPLACE, LOCK=NONE',
              'DO 0');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.STATISTICS
               WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'thread' AND INDEX_NAME = 'idx_thread_portal_status_surface_created') = 0,
              'ALTER TABLE thread ADD INDEX idx_thread_portal_status_surface_created (portal_id, status, surface, created_at DESC), ALGORITHM=INPLACE, LOCK=NONE',
              'DO 0');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.STATISTICS
               WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'thread' AND INDEX_NAME = 'idx_thread_portal_status_hot') = 0,
              'ALTER TABLE thread ADD INDEX idx_thread_portal_status_hot (portal_id, status, hot_score DESC), ALGORITHM=INPLACE, LOCK=NONE',
              'DO 0');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.STATISTICS
               WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'thread' AND INDEX_NAME = 'idx_thread_portal_status_surface_hot') = 0,
              'ALTER TABLE thread ADD INDEX idx_thread_portal_status_surface_hot (portal_id, status, surface, hot_score DESC), ALGORITHM=INPLACE, LOCK=NONE',
              'DO 0');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

-- 3f) Trending için comment indeksi (son 7 gün yorum sayısı: created_at aralığı). Mevcut idx_comment_* hiçbiri
--     created_at ile BAŞLAMıyor/aralık için kullanılamıyor -> yorum tablosunda tam tarama olurdu.
--     Kapsayıcı (covering): status eşitlik, created_at aralık, subject_type/subject_id aynı indeksten okunur, thread'e PK ile join.
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.STATISTICS
               WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'comment' AND INDEX_NAME = 'idx_comment_status_created_subject') = 0,
              'ALTER TABLE comment ADD INDEX idx_comment_status_created_subject (status, created_at DESC, subject_type, subject_id), ALGORITHM=INPLACE, LOCK=NONE',
              'DO 0');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

-- 3g) OPSİYONEL (varsayılan KAPALI) — trending'in "yeni thread sayısı" terimi için kapsayıcı indeks.
--     Sadece EXPLAIN'de idx_thread_status_created + satır lookup'ı yavaş çıkarsa aç (7 günde binlerce+ thread).
--     idx_thread_status_created'ın YERİNE KOYMA: portal_id, created_at ile id arasına girdiği için feed'in
--     (created_at DESC, id ASC) sıralaması filesort'a düşer.
-- ALTER TABLE thread ADD INDEX idx_thread_status_created_portal (status, created_at DESC, portal_id), ALGORITHM=INPLACE, LOCK=NONE;

-- 3h) OPSİYONEL (varsayılan KAPALI) — portal içi "top" (like_count) sıralaması sık kullanılırsa:
-- ALTER TABLE thread ADD INDEX idx_thread_portal_status_likes (portal_id, status, like_count DESC), ALGORITHM=INPLACE, LOCK=NONE;
-- Yokken de doğru çalışır: optimizer idx_thread_portal_status_created'dan portal+status aralığını okuyup o portalın
-- satırlarını filesort eder (portal boyutuyla sınırlı, global tarama değil).

-- =====================================================================
-- ADIM 4 — Sayaç backfill (denormalize sayaçlar; idempotent, her çalıştırmada gerçek değere eşitler)
-- =====================================================================
-- thread_count yalnız PUBLISHED. updated_at'a dokunulmaz. Uygulama DURUKEN çalıştır (canlı join/thread ile yarışır).
UPDATE portal p
SET p.thread_count = (SELECT COUNT(*) FROM thread t WHERE t.portal_id = p.id AND t.status = 'PUBLISHED'),
    p.member_count = (SELECT COUNT(*) FROM portal_membership m WHERE m.portal_id = p.id);

-- =====================================================================
-- ADIM 5 — Doğrulama (uygulama açılmadan önce)
-- =====================================================================
-- SELECT COUNT(*) FROM thread WHERE portal_id IS NULL;                       -- 0
-- SELECT COUNT(*) FROM thread t LEFT JOIN portal p ON p.id = t.portal_id WHERE p.id IS NULL;   -- 0 (yetim yok)
-- SELECT p.slug, p.thread_count, (SELECT COUNT(*) FROM thread t WHERE t.portal_id=p.id AND t.status='PUBLISHED') AS real_cnt
--   FROM portal p HAVING p.thread_count <> real_cnt;                          -- boş
-- SELECT p.slug, p.member_count, (SELECT COUNT(*) FROM portal_membership m WHERE m.portal_id=p.id) AS real_cnt
--   FROM portal p HAVING p.member_count <> real_cnt;                          -- boş
-- Bilinen veri kirliliği: production_slug'ı DOLU ama hiçbir portala bağlı olmayan eski thread'ler genel-sohbet'e düştü;
-- yeni kural (portalın yapımı olmalı) bunlarda ihlal görünür. Sayısı:
-- SELECT COUNT(*) FROM thread t JOIN portal p ON p.id=t.portal_id
--   WHERE p.slug='genel-sohbet' AND t.production_slug IS NOT NULL;
-- Sık gelen iddiaları EXPLAIN ile doğrula (type=ref/range, Extra'da "Using filesort" OLMAMALI):
-- EXPLAIN SELECT id FROM thread WHERE portal_id=1 AND status='PUBLISHED' ORDER BY created_at DESC, id ASC LIMIT 21;
-- EXPLAIN SELECT id FROM thread WHERE portal_id=1 AND status='PUBLISHED' AND surface='THEORY' ORDER BY hot_score DESC, id ASC LIMIT 21;
-- EXPLAIN SELECT t.portal_id, COUNT(*) FROM comment c JOIN thread t ON c.subject_type='THREAD' AND t.id=c.subject_id
--   WHERE c.status='PUBLISHED' AND c.created_at >= NOW() - INTERVAL 7 DAY GROUP BY t.portal_id;

-- =====================================================================
-- İNDEKS GEREKÇESİ — önerilenler vs mevcut idx_thread_*
-- =====================================================================
-- Mevcut (Thread.java): (status,surface,hot|created|likes), (status,hot|created|likes), (status,production_slug,hot|created),
-- (author_id,surface,status). Hiçbiri portal_id içermiyor; portal_id=? eşitliği olan sorguda optimizer ya global
-- (status, x) indeksini tarayıp portal_id'yi satır lookup ile süzer (ilk 20 portal-satırı için potansiyel olarak
-- tüm tabloyu gezer) ya da portal boyutunu filesort eder -> portal_id ÖNEKLİ indeks şart.
--
-- Sözleşmedeki öneri ile FARKLAR:
--  * (portalId, surface, createdAt) -> (portal_id, status, surface, created_at). Sorgu daima status='PUBLISHED' içerir
--    (Thread listeleri PUBLISHED-only); status indekste yoksa her aday satır için tablo lookup gerekir (ICP bile
--    satırı okumadan eleyemez). Eşitlik sütunları sırayla başta, ORDER BY sütunu sonda = mevcut kural.
--  * surface'siz akış (`/portals/{slug}/feed` surface vermeden) (portal_id,status,surface,created) ile sıralanamaz (araya
--    surface girer, aynı neden mevcut şemada idx_thread_status_created'ın ayrı var olması) -> (portal_id,status,created_at)
--    ayrıca tutulur. Aynı ikili hot için de: idx_thread_portal_status_hot + _surface_hot.
--  * Sözleşmedeki 3 indeks yerine 4 (+opsiyonel 2). Hot çifti HotScoreJob yazma maliyetini artırır: hot_score zaten 3
--    indekste (500k satırda ~78 sn), +2 indeks = 5 indeks. Job yalnız DEĞERİ DEĞİŞEN satırların indeks girişini
--    günceller ama hot_score zamanla her satırda değişir (yaş paydada) -> maliyeti ~%60 artırabilir. Ödünleşim: yalnız
--    idx_thread_portal_status_hot (surface'siz) tutup surface_hot'u atmak, hot+surface sorgusunu portal içi surface
--    süzmesine bırakmak; 3 surface değeri varken sonuç ~%33 seçicilik -> ilk 20 satır için ~60 satır okur, ucuz —
--    ANCAK bir portalda nadir bir surface (ör. FAN_ART %1) için portalın tamamını gezebilir. Bu yüzden surface_hot
--    varsayılan AÇIK bırakıldı; HotScoreJob süresi ölçümde kabul edilemezse bu indeksi DROP et:
--      ALTER TABLE thread DROP INDEX idx_thread_portal_status_surface_hot;
--  * created_at indeksleri güncellenmez (created_at değişmez) -> yazma maliyeti yalnız thread INSERT'inde.
--  * Ayrı (portal_id) indeksi YOK: FK, portal_id önekli herhangi bir indeksi yeniden kullanır.
--  * scope=joined (portal_id IN (üye portallar) ORDER BY created_at DESC): IN çoklu-aralık olduğundan portal_id-önekli
--    indeksler global sırayı vermez -> filesort (üye olunan portalların satırları). MySQL bunun yerine
--    idx_thread_status_created'ı tarayıp portal_id'yi lookup ile de süzebilir; hangisinin seçileceğini optimizer
--    istatistiklerle belirler. Yüksek üyelik/çok portal durumunda çözüm indeks değil: portal başına ilk N satırı
--    UNION ALL + (portal_id, status, created_at) ile çekmek (keyset ile birleşir). Şimdilik ek indeks önerilmez.
--
-- TRENDING (sort=trending) indeks önerisi:
--  Formül servis katmanında TEK yerde (öneri: score = 3 * new_threads_7d + comments_7d; ağırlıklar servise ait, indeks
--  bağımsız). İki alt sorgu:
--   (1) yeni thread: WHERE status='PUBLISHED' AND created_at >= :since GROUP BY portal_id
--       -> mevcut idx_thread_status_created aralığı okur ama portal_id için satır lookup yapar (7 günlük satır sayısı kadar).
--          Genelde yeterli; ağırsa 3g'deki kapsayıcı idx_thread_status_created_portal (Using index).
--   (2) yorum: comment tablosunda created_at aralığı + thread'e join. Mevcut comment indekslerinin hiçbiri
--       created_at ile başlamaz (subject_type, subject_id, parent_id, created_at...) -> aralık için kullanılamaz, tam
--       tarama. -> idx_comment_status_created_subject (3f, AKTİF): status eşitlik + created_at aralık + subject_type/
--       subject_id kapsayıcı, thread'e PK ile join (yalnız subject_type='THREAD' satırlar anlamlı).
--  Maliyet: her istek son 7 günün TÜM yorumlarını sayar (portal sayısından bağımsız, yorum hacmine orantılı). Yorum
--  hacmi ~1M/hafta'yı geçerse (veya p95 hedefi tutmazsa) denormalize `portal.activity_7d` + job'a geçmeden ÖNCE
--  yalnız-anonim, kısa TTL'li (ör. 60 sn, sync=true) bir cache dene: skor herkes için aynı (isMember ayrı çözülür).
--  Bu karar için ÖNCE SORULMALI; bu migration denormalize alan eklemez.

-- =====================================================================
-- ESKİ / YENİDEN ADLANDIRILAN İNDEKSLER
-- =====================================================================
-- Bu migration mevcut hiçbir indeksi yeniden adlandırmaz/kaldırmaz -> DROP gerekmez. ddl-auto=update yine de eski
-- indeksleri silmez; entity'deki @Index adları yukarıdakilerden farklı çıkarsa aynı kolonlarda kopya oluşur. Tespit:
-- SELECT INDEX_NAME, GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX) AS cols
--   FROM information_schema.STATISTICS
--  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'thread' GROUP BY INDEX_NAME ORDER BY cols;
-- Aynı cols'lu ikinci satır kopyadır (sözleşmenin orijinal adlarıyla Hibernate açtıysa), gereksiz olanı DROP et:
-- ALTER TABLE thread DROP INDEX <kopya_ad>;
-- Ayrıca sözleşmedeki (portal_id, surface, created_at) indeksi (status'suz) bir yerde oluşmuşsa DROP et.

-- =====================================================================
-- ROLLBACK
-- =====================================================================
-- (A) Yumuşak rollback — ESKİ build'e dön, veri/şema kalsın (önerilen, veri kaybı YOK):
--     Eski build thread INSERT'inde portal_id vermez; NOT NULL iken INSERT'ler kırılır. Önce:
-- ALTER TABLE thread MODIFY COLUMN portal_id BIGINT NULL;
--     Eski build portal tablolarını görmez; yeni build'e dönerken ADIM 3b-3c'yi (idempotent) yeniden çalıştır:
--     eski build'in yazdığı portal_id'siz thread'ler genel-sohbet'e backfill edilir, sonra ADIM 4 sayaçları düzeltir.
-- (B) Sert rollback — Portal katmanını tamamen kaldır (PORTAL ATAMALARI VE ÜYELİKLER KAYBOLUR; thread'ler kalır):
-- ALTER TABLE thread DROP FOREIGN KEY fk_thread_portal;
-- ALTER TABLE thread DROP INDEX idx_thread_portal_status_created,
--                    DROP INDEX idx_thread_portal_status_surface_created,
--                    DROP INDEX idx_thread_portal_status_hot,
--                    DROP INDEX idx_thread_portal_status_surface_hot;
-- ALTER TABLE thread DROP COLUMN portal_id;
-- ALTER TABLE comment DROP INDEX idx_comment_status_created_subject;
-- DROP TABLE portal_membership;
-- DROP TABLE portal_production;
-- DROP TABLE portal;
-- Sert rollback öncesi yedek: mysqldump fandoom thread portal portal_production portal_membership
