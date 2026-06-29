SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

CREATE TABLE IF NOT EXISTS chat_import_batch (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    group_id VARCHAR(64) NOT NULL,
    source_file VARCHAR(512) NOT NULL,
    source_hash VARCHAR(128) NOT NULL,
    chat_name VARCHAR(255) NULL,
    exporter_name VARCHAR(128) NULL,
    exporter_version VARCHAR(64) NULL,
    start_time DATETIME NULL,
    end_time DATETIME NULL,
    total_messages BIGINT NOT NULL DEFAULT 0,
    raw_count BIGINT NOT NULL DEFAULT 0,
    clean_count BIGINT NOT NULL DEFAULT 0,
    mention_count BIGINT NOT NULL DEFAULT 0,
    reply_count BIGINT NOT NULL DEFAULT 0,
    session_count BIGINT NOT NULL DEFAULT 0,
    member_count BIGINT NOT NULL DEFAULT 0,
    status VARCHAR(32) NOT NULL DEFAULT 'STARTED',
    error_message TEXT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_chat_import_hash_status (group_id, source_hash, status),
    KEY idx_chat_import_group_created (group_id, created_at)
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS chat_raw_message (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    batch_id BIGINT NOT NULL,
    group_id VARCHAR(64) NOT NULL,
    message_id VARCHAR(128) NOT NULL,
    seq BIGINT NULL,
    message_time DATETIME NULL,
    sender_uid VARCHAR(128) NULL,
    sender_uin VARCHAR(64) NULL,
    sender_name VARCHAR(255) NULL,
    sender_group_card VARCHAR(255) NULL,
    message_type VARCHAR(64) NULL,
    raw_text TEXT NULL,
    system_flag TINYINT(1) NOT NULL DEFAULT 0,
    recalled_flag TINYINT(1) NOT NULL DEFAULT 0,
    has_resource TINYINT(1) NOT NULL DEFAULT 0,
    element_types VARCHAR(512) NULL,
    raw_json LONGTEXT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_chat_raw_batch_message (batch_id, message_id),
    KEY idx_chat_raw_batch_time (batch_id, message_time, seq),
    KEY idx_chat_raw_group_sender (group_id, sender_uid, sender_uin)
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS chat_clean_message (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    raw_message_id BIGINT NOT NULL,
    batch_id BIGINT NOT NULL,
    group_id VARCHAR(64) NOT NULL,
    message_id VARCHAR(128) NOT NULL,
    seq BIGINT NULL,
    message_time DATETIME NULL,
    sender_uid VARCHAR(128) NULL,
    sender_uin VARCHAR(64) NULL,
    sender_name VARCHAR(255) NULL,
    clean_text TEXT NOT NULL,
    text_length INT NOT NULL DEFAULT 0,
    is_reply TINYINT(1) NOT NULL DEFAULT 0,
    has_mention TINYINT(1) NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_chat_clean_raw (raw_message_id),
    KEY idx_chat_clean_batch_time (batch_id, message_time, seq),
    KEY idx_chat_clean_group_sender (group_id, sender_uid, sender_uin)
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS chat_message_mention (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    raw_message_id BIGINT NOT NULL,
    group_id VARCHAR(64) NOT NULL,
    message_id VARCHAR(128) NOT NULL,
    sender_uid VARCHAR(128) NULL,
    mentioned_uid VARCHAR(128) NULL,
    mentioned_name VARCHAR(255) NULL,
    mention_type VARCHAR(32) NOT NULL DEFAULT 'AT',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_chat_mention_raw (raw_message_id),
    KEY idx_chat_mention_target (group_id, mentioned_uid)
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS chat_message_reply (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    raw_message_id BIGINT NOT NULL,
    group_id VARCHAR(64) NOT NULL,
    message_id VARCHAR(128) NOT NULL,
    reply_message_id VARCHAR(128) NULL,
    reply_sender_uin VARCHAR(64) NULL,
    reply_sender_name VARCHAR(255) NULL,
    reply_content TEXT NULL,
    reply_time DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_chat_reply_raw (raw_message_id),
    KEY idx_chat_reply_target (group_id, reply_message_id)
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS chat_session (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    batch_id BIGINT NOT NULL,
    group_id VARCHAR(64) NOT NULL,
    session_no INT NOT NULL,
    start_time DATETIME NULL,
    end_time DATETIME NULL,
    message_count INT NOT NULL DEFAULT 0,
    member_count INT NOT NULL DEFAULT 0,
    summary TEXT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_chat_session_no (batch_id, session_no),
    KEY idx_chat_session_group_time (group_id, start_time)
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS chat_session_message (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    session_id BIGINT NOT NULL,
    clean_message_id BIGINT NOT NULL,
    message_order INT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_chat_session_message (session_id, clean_message_id),
    KEY idx_chat_session_message_order (session_id, message_order)
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS chat_member_stat (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    batch_id BIGINT NOT NULL,
    group_id VARCHAR(64) NOT NULL,
    sender_uid VARCHAR(128) NULL,
    sender_uin VARCHAR(64) NULL,
    sender_name VARCHAR(255) NULL,
    raw_message_count BIGINT NOT NULL DEFAULT 0,
    message_count BIGINT NOT NULL DEFAULT 0,
    active_days INT NOT NULL DEFAULT 0,
    mention_count BIGINT NOT NULL DEFAULT 0,
    reply_count BIGINT NOT NULL DEFAULT 0,
    replied_by_count BIGINT NOT NULL DEFAULT 0,
    session_count BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_chat_member_stat (batch_id, sender_uid, sender_uin, sender_name),
    KEY idx_chat_member_stat_group (group_id, message_count)
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS chat_member_stat_daily (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    batch_id BIGINT NOT NULL,
    group_id VARCHAR(64) NOT NULL,
    stat_date DATE NOT NULL,
    sender_uid VARCHAR(128) NULL,
    sender_uin VARCHAR(64) NULL,
    sender_name VARCHAR(255) NULL,
    raw_message_count BIGINT NOT NULL DEFAULT 0,
    message_count BIGINT NOT NULL DEFAULT 0,
    active_days INT NOT NULL DEFAULT 0,
    mention_count BIGINT NOT NULL DEFAULT 0,
    reply_count BIGINT NOT NULL DEFAULT 0,
    replied_by_count BIGINT NOT NULL DEFAULT 0,
    session_count BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_chat_member_stat_daily (batch_id, stat_date, sender_uid, sender_uin, sender_name),
    KEY idx_chat_member_stat_daily_group_date (group_id, stat_date),
    KEY idx_chat_member_stat_daily_message (group_id, message_count)
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
