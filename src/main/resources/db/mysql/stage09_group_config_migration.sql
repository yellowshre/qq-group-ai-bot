SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;
SET CHARACTER SET utf8mb4;

SET @schema_name = DATABASE();

SET @sql = IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'group_config' AND COLUMN_NAME = 'enable_meme') = 0,
    'ALTER TABLE group_config ADD COLUMN enable_meme TINYINT(1) NOT NULL DEFAULT 1 AFTER enable_chat',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'group_config' AND COLUMN_NAME = 'enable_passive_chat') = 0,
    'ALTER TABLE group_config ADD COLUMN enable_passive_chat TINYINT(1) NOT NULL DEFAULT 1 AFTER enable_meme',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'group_config' AND COLUMN_NAME = 'active_cooldown_seconds') = 0,
    'ALTER TABLE group_config ADD COLUMN active_cooldown_seconds BIGINT NOT NULL DEFAULT 180 AFTER enable_auto_join',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'group_config' AND COLUMN_NAME = 'active_hour_limit') = 0,
    'ALTER TABLE group_config ADD COLUMN active_hour_limit BIGINT NOT NULL DEFAULT 20 AFTER active_cooldown_seconds',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'group_config' AND COLUMN_NAME = 'active_day_limit') = 0,
    'ALTER TABLE group_config ADD COLUMN active_day_limit BIGINT NOT NULL DEFAULT 80 AFTER active_hour_limit',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
