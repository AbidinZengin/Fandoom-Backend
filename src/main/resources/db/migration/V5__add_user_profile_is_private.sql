-- Gizli profil: user_profile.is_private (varsayilan 0 = herkese acik).
-- MySQL DDL transaction'a girmez; yarim kalirsa yeniden calistirilabilsin diye kolon
-- yalnizca yoksa eklenir (information_schema + PREPARE, MySQL'de ADD COLUMN IF NOT EXISTS yok).
SET @sql = (SELECT IF(
    EXISTS (SELECT 1 FROM information_schema.columns
            WHERE table_schema = DATABASE() AND table_name = 'user_profile' AND column_name = 'is_private'),
    'DO 0',
    'ALTER TABLE user_profile ADD COLUMN is_private BIT(1) NOT NULL DEFAULT b''0'''));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
