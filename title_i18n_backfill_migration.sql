-- Title/Synopsis/... i18n temizlik script'i
--
-- Karar degisikligi: ayri bir "_en" kolonu YOK artik. Varsayilan dil zaten
-- Ingilizce oldugu icin entity'ler eskisi gibi TEK bir varsayilan alan
-- (title, synopsis, kicker, axis, imageAlt, storyKicker, storyTitle,
-- storyThesis, sceneKicker, content, mediaAlt, text, name) + opsiyonel
-- *_tr alanina donduruldu. Bu alanlar zaten eski (split-oncesi) kolonlarla
-- ayni isimde oldugu icin entity'ler dogrudan o kolonlara yeniden baglandi
-- -- veri tasima GEREKMIYOR, sadece artik kullanilmayan "_en" kolonlarini
-- temizliyoruz.
--
-- İSTİSNA: series_hero_block hic "text" kolonuyla kurulmamisti (sadece
-- text_en/text_tr vardi), o yuzden once mevcut veriyi koruyarak rename
-- ediyoruz.

ALTER TABLE series_hero_block CHANGE COLUMN text_en text TEXT NULL;

ALTER TABLE movie DROP COLUMN title_en, DROP COLUMN synopsis_en;
ALTER TABLE series DROP COLUMN title_en, DROP COLUMN synopsis_en;
ALTER TABLE season DROP COLUMN title_en;
ALTER TABLE episode DROP COLUMN title_en, DROP COLUMN synopsis_en,
    DROP COLUMN story_kicker_en, DROP COLUMN story_title_en, DROP COLUMN story_thesis_en;
ALTER TABLE episode_block DROP COLUMN scene_kicker_en, DROP COLUMN content_en, DROP COLUMN media_alt_en;
ALTER TABLE blog DROP COLUMN title_en, DROP COLUMN kicker_en, DROP COLUMN axis_en, DROP COLUMN image_alt_en;
ALTER TABLE blog_block DROP COLUMN text_en, DROP COLUMN image_alt_en;
ALTER TABLE genre DROP COLUMN name_en;
