-- BlogBlock -> SeasonBlock sistemine geçiş veri taşıma script'i
-- (kullanıcı kararı, 2026-08: "BlogBlock kısmını SeasonBlock ile aynı
-- sisteme getirmen" — sceneKey grouping + content/contentTr isimlendirmesi,
-- serbest x/y/width/height canvas'ın YERİNE geçiyor. blockType/görsel alan
-- isimleri KASITLI olarak Season'dan farklı: MEDIA değil IMAGE, mediaUrl
-- değil imageUrl/imageAlt(Tr), mediaCredit YOK — bkz. BlogBlock.java yorumu).
--
-- 2. SÜRÜM — gerçek canlı şemaya göre düzeltildi (ilk sürüm ADIM 1'de
-- "Duplicate column name 'scene_key'" ile durdu: backend arkada bir kez
-- otomatik yeniden başlamış, Hibernate ddl-auto=update scene_key/content/
-- content_tr'yi VE (o sıradaki ara taslağımdan) media_url/media_alt(Tr)/
-- media_credit'i zaten eklemiş — hiçbiri veri taşımadı, sadece boş kolon
-- ekledi. x/y/width/height/animation/font_family/font_scale ve
-- blog.canvas_height ise DB'de HİÇ VAR OLMAMIŞ (Java'da eklenip araya
-- restart girmeden kaldırılmışlar) — bu sürümde onlara dokunulmuyor.
--
-- OTOMATİK ÇALIŞTIRILMADI, elle/manuel çalıştırıldı (bkz. konuşma geçmişi).

-- =========================================================
-- ADIM 1 — block_type ENUM'unu ESKİ+YENİ değerlerin BİRLEŞİMİNE genişlet.
-- MySQL ENUM'dan bir değeri, o değeri kullanan satırlar hâlâ dururken
-- silersen satırlar sessizce '' olur — bu yüzden önce genişletip veriyi
-- taşıdıktan SONRA (ADIM 3) daraltıyoruz.
-- =========================================================
ALTER TABLE blog_block MODIFY COLUMN block_type
  ENUM('HEADING','IMAGE','PARAGRAPH','QUOTE',
       'LEDE_TEXT','SECTION_HEADING','SECTION_TEXT','SECTION_LEAD_TEXT','VERDICT_TEXT')
  NOT NULL;

-- =========================================================
-- ADIM 2 — Mevcut veriyi yeni kolonlara taşı / eşle.
-- =========================================================

-- content/content_tr <- text/text_tr (yalnızca isim değişti, veri aynen taşınır)
UPDATE blog_block SET content = text, content_tr = text_tr;

-- scene_key: eski şemada sahne/section kavramı yoktu (her blok bağımsız,
-- serbest x/y konumluydu) — her bloğa KENDİ BAŞINA bir sahne anahtarı
-- üretiliyor (order_index content_block'ta, blog_block'ta DEĞİL — JOINED
-- inheritance). Bu, editöryel açıdan anlamlı bir gruplama SAĞLAMAZ —
-- yayınlanmış yazıların gerçek sahne gruplaması editör tarafından ELLE
-- gözden geçirilip yeniden düzenlenmeli.
UPDATE blog_block bb
JOIN content_block cb ON cb.id = bb.id
SET bb.scene_key = CONCAT('scene-', bb.blog_id, '-', cb.order_index);

-- block_type eşleme: PARAGRAPH->SECTION_TEXT, HEADING->SECTION_HEADING.
-- IMAGE ve QUOTE isim olarak AYNI kaldı, eşleme gerekmiyor.
UPDATE blog_block SET block_type = 'SECTION_TEXT' WHERE block_type = 'PARAGRAPH';
UPDATE blog_block SET block_type = 'SECTION_HEADING' WHERE block_type = 'HEADING';

-- =========================================================
-- ADIM 3 — block_type ENUM'unu NİHAİ (sadece yeni) değerlere daralt.
-- =========================================================
ALTER TABLE blog_block MODIFY COLUMN block_type
  ENUM('LEDE_TEXT','SECTION_HEADING','SECTION_TEXT','SECTION_LEAD_TEXT','IMAGE','QUOTE','VERDICT_TEXT')
  NOT NULL;

-- =========================================================
-- ADIM 4 — Artık kullanılmayan kolonları kaldır.
-- text/text_tr: content/content_tr'ye taşındı.
-- media_url/media_alt/media_alt_tr/media_credit: ara bir taslaktan kalan,
-- hiç veri taşımadı (hepsi NULL) — kullanıcı kararı: blockType IMAGE kalıp
-- image_url/image_alt(Tr) kullanılacak, media_* hiç gerekmiyor.
-- =========================================================
ALTER TABLE blog_block
  DROP COLUMN text_tr,
  DROP COLUMN text,
  DROP COLUMN media_url,
  DROP COLUMN media_alt_tr,
  DROP COLUMN media_alt,
  DROP COLUMN media_credit;

-- =========================================================
-- ADIM 5 — Doğrula.
-- Backend'i yeni kodla yeniden başlat (şu an hâlâ ara bir taslakla ayakta),
-- GET /api/blogs/{slug} ile mevcut yazıların gövdesinin (artık lede/
-- section/verdict blok tipleriyle) döndüğünü kontrol et.
-- =========================================================
