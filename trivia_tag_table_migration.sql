-- trivia_tag_table_migration.sql
-- Trivia etiketleri artık kapalı bir küme: `trivia_tag` tablosu (code + labelEn + labelTr). POST/PUT'ta
-- yalnız bu tablodaki kodlar kabul edilir (bilinmeyen kod 400). Trivia tarafında etiketler `trivia_tags`
-- tablosunda düz string olarak kalır; bu script o sütunu `trivia_tag.code`'a FK ile bağlar.
-- Bu script:
--   1) trivia_tag tablosunu (Hibernate ile aynı şemada) yoksa oluşturur,
--   2) 10 başlangıç etiketini ekler (5 hazır + 5 mevcut kayıtlarda geçen),
--   3) mevcut kayıtlarda geçen ama tabloda olmayan her etiketi satır olarak ekler (FK kurulurken patlamasın),
--   4) trivia_tags.tag → trivia_tag.code FK'sını (yoksa) kurar.
-- İDEMPOTENT: tekrar çalıştırmak güvenli. Yeni build'in İLK açılışından önce veya sonra çalıştırılabilir
-- (ddl-auto=update tabloyu zaten oluşturur; kapalı küme olduğu için satırlar eklenene kadar POST/PUT etiket
-- doğrulamasında 400 döner, mevcut kayıtların okunması etkilenmez).
-- Yeni etiket eklemek için: INSERT INTO trivia_tag (code, label_en, label_tr) VALUES (...);

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS trivia_tag (
    id       BIGINT       NOT NULL AUTO_INCREMENT,
    code     VARCHAR(30)  NOT NULL,
    label_en VARCHAR(60)  NOT NULL,
    label_tr VARCHAR(60)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_trivia_tag_code UNIQUE (code)
);

-- Kod biçimi: hazır etiketler BÜYÜK_HARF_ALT_ÇİZGİ (FE ikon eşlemesi bunlara bakar); diğerleri mevcut
-- kayıtlardaki yazımıyla birebir (FK bu değerlerle eşleşmeli).
INSERT IGNORE INTO trivia_tag (code, label_en, label_tr) VALUES
    ('BEHIND_THE_SCENES', 'Behind the Scenes', 'Kamera Arkası'),
    ('EASTER_EGG',        'Easter Egg',        'Easter Egg'),
    ('CASTING',           'Casting',           'Oyuncu Seçimi'),
    ('GOOF',              'Goof',              'Hata'),
    ('LORE',              'Lore',              'Lore'),
    ('Costume',           'Costume',           'Kostüm'),
    ('Music',             'Music',             'Müzik'),
    ('Reception',         'Reception',         'Yankı'),
    ('Legacy',            'Legacy',            'Miras'),
    ('Cinematography',    'Cinematography',    'Görüntü Yönetimi');

-- Mevcut trivia_tags'te geçen ama yukarıda olmayan etiketler (başka yapımların kayıtları vb.): kodun kendisi
-- her iki dilde ad olur; sonradan label_en/label_tr elle düzeltilebilir.
INSERT IGNORE INTO trivia_tag (code, label_en, label_tr)
SELECT DISTINCT tag, tag, tag FROM trivia_tags;

SET @has_fk := (SELECT COUNT(*) FROM information_schema.table_constraints
                WHERE table_schema = DATABASE() AND table_name = 'trivia_tags'
                  AND constraint_name = 'fk_trivia_tags_tag' AND constraint_type = 'FOREIGN KEY');

-- ON UPDATE CASCADE: etiket kodu yeniden adlandırılırsa kayıtlar izler. ON DELETE varsayılan (RESTRICT):
-- kullanımda olan etiket silinemez.
SET @add_fk := IF(@has_fk = 0,
    'ALTER TABLE trivia_tags ADD CONSTRAINT fk_trivia_tags_tag FOREIGN KEY (tag) REFERENCES trivia_tag (code) ON UPDATE CASCADE',
    'SELECT 1');
PREPARE stmt FROM @add_fk; EXECUTE stmt; DEALLOCATE PREPARE stmt;
