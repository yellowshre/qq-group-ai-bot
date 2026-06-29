SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;
SET CHARACTER SET utf8mb4;
ALTER DATABASE qqbot CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS group_config (
    group_id BIGINT PRIMARY KEY,
    bot_on TINYINT(1) NOT NULL DEFAULT 1,
    enable_chat TINYINT(1) NOT NULL DEFAULT 1,
    enable_meme TINYINT(1) NOT NULL DEFAULT 1,
    enable_passive_chat TINYINT(1) NOT NULL DEFAULT 1,
    enable_auto_join TINYINT(1) NOT NULL DEFAULT 0,
    active_cooldown_seconds BIGINT NOT NULL DEFAULT 180,
    active_hour_limit BIGINT NOT NULL DEFAULT 20,
    active_day_limit BIGINT NOT NULL DEFAULT 80,
    safe_word VARCHAR(128) NULL,
    safe_word_reply VARCHAR(512) NOT NULL DEFAULT '收到，我先安静一下。',
    persona VARCHAR(1024) NOT NULL DEFAULT '',
    memory_mode VARCHAR(16) NOT NULL DEFAULT 'SHORT',
    enable_knowledge_context TINYINT(1) NOT NULL DEFAULT 0,
    enable_meme_knowledge TINYINT(1) NOT NULL DEFAULT 0,
    enable_passive_chat_knowledge TINYINT(1) NOT NULL DEFAULT 0,
    enable_active_chat_knowledge TINYINT(1) NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS scene_dict (
    scene_code VARCHAR(64) PRIMARY KEY,
    scene_desc VARCHAR(255) NOT NULL,
    confidence_threshold DECIMAL(5,4) NOT NULL DEFAULT 0.7500
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS meme_material (
    meme_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    keywords VARCHAR(1024) NOT NULL DEFAULT '',
    scene_code VARCHAR(64) NULL,
    scene_desc VARCHAR(255) NULL,
    weight INT NOT NULL DEFAULT 1,
    enabled TINYINT(1) NOT NULL DEFAULT 1,
    file_path VARCHAR(1024) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_meme_scene_enabled (scene_code, enabled),
    UNIQUE KEY uk_meme_file_path (file_path(255))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS chat_summary (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    group_id BIGINT NOT NULL,
    summary_text TEXT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_chat_summary_group_created (group_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS trigger_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    group_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    message_id VARCHAR(128) NOT NULL,
    original_msg TEXT NULL,
    response_type VARCHAR(32) NOT NULL,
    response_text TEXT NULL,
    meme_id BIGINT NULL,
    workflow_type VARCHAR(32) NULL,
    token_used INT NULL,
    cost DECIMAL(12,6) NULL,
    duration_ms BIGINT NULL,
    success TINYINT(1) NOT NULL DEFAULT 1,
    error_msg TEXT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_trigger_log_group_created (group_id, created_at),
    INDEX idx_trigger_log_message (message_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS admin_op_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    group_id BIGINT NOT NULL,
    operator_uid BIGINT NOT NULL,
    operation VARCHAR(64) NOT NULL,
    detail TEXT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_admin_op_group_created (group_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO scene_dict (scene_code, scene_desc, confidence_threshold)
VALUES
    ('laugh', '好笑、调侃、哈哈哈的场景', 0.7500),
    ('angry', '生气、暴躁、吐槽的场景', 0.7500),
    ('confused', '疑惑、看不懂、震惊的场景', 0.7500)
ON DUPLICATE KEY UPDATE
    scene_desc = VALUES(scene_desc),
    confidence_threshold = VALUES(confidence_threshold);

INSERT INTO meme_material (keywords, scene_code, scene_desc, weight, enabled, file_path)
SELECT '哈哈,笑死,乐', 'laugh', '好笑、调侃、哈哈哈的场景', 10, 1, 'laugh/laugh_001.png'
WHERE NOT EXISTS (SELECT 1 FROM meme_material WHERE file_path = 'laugh/laugh_001.png');

INSERT INTO meme_material (keywords, scene_code, scene_desc, weight, enabled, file_path)
SELECT '绷不住,离谱', 'laugh', '好笑、调侃、哈哈哈的场景', 6, 1, 'laugh/laugh_002.png'
WHERE NOT EXISTS (SELECT 1 FROM meme_material WHERE file_path = 'laugh/laugh_002.png');

INSERT INTO meme_material (keywords, scene_code, scene_desc, weight, enabled, file_path)
SELECT '生气,怒了,气死', 'angry', '生气、暴躁、吐槽的场景', 8, 1, 'angry/angry_001.png'
WHERE NOT EXISTS (SELECT 1 FROM meme_material WHERE file_path = 'angry/angry_001.png');

INSERT INTO meme_material (keywords, scene_code, scene_desc, weight, enabled, file_path)
SELECT '啊?,啥,看不懂', 'confused', '疑惑、看不懂、震惊的场景', 7, 1, 'confused/confused_001.png'
WHERE NOT EXISTS (SELECT 1 FROM meme_material WHERE file_path = 'confused/confused_001.png');
