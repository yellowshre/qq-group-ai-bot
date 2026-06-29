SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;
SET CHARACTER SET utf8mb4;

SET @schema_name = DATABASE();

SET @sql = IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'group_config' AND COLUMN_NAME = 'enable_knowledge_context') = 0,
    'ALTER TABLE group_config ADD COLUMN enable_knowledge_context TINYINT(1) NOT NULL DEFAULT 0 AFTER memory_mode',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'group_config' AND COLUMN_NAME = 'enable_meme_knowledge') = 0,
    'ALTER TABLE group_config ADD COLUMN enable_meme_knowledge TINYINT(1) NOT NULL DEFAULT 0 AFTER enable_knowledge_context',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'group_config' AND COLUMN_NAME = 'enable_passive_chat_knowledge') = 0,
    'ALTER TABLE group_config ADD COLUMN enable_passive_chat_knowledge TINYINT(1) NOT NULL DEFAULT 0 AFTER enable_meme_knowledge',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'group_config' AND COLUMN_NAME = 'enable_active_chat_knowledge') = 0,
    'ALTER TABLE group_config ADD COLUMN enable_active_chat_knowledge TINYINT(1) NOT NULL DEFAULT 0 AFTER enable_passive_chat_knowledge',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
