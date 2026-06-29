SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

CREATE TABLE IF NOT EXISTS chat_group_knowledge (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    group_id VARCHAR(64) NOT NULL,
    source_candidate_id BIGINT NOT NULL,
    knowledge_type VARCHAR(32) NOT NULL,
    title VARCHAR(100) NOT NULL,
    content TEXT NOT NULL,
    evidence_text TEXT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    enabled TINYINT(1) NOT NULL DEFAULT 1,
    version INT NOT NULL DEFAULT 1,
    created_by VARCHAR(128) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_chat_group_knowledge_source (source_candidate_id),
    KEY idx_chat_group_knowledge_group (group_id, enabled, status),
    KEY idx_chat_group_knowledge_type (group_id, knowledge_type)
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS chat_member_profile (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    group_id VARCHAR(64) NOT NULL,
    source_member_candidate_id BIGINT NOT NULL,
    sender_uid VARCHAR(128) NULL,
    sender_uin VARCHAR(64) NULL,
    sender_name VARCHAR(255) NULL,
    profile_text TEXT NOT NULL,
    message_count BIGINT NOT NULL DEFAULT 0,
    raw_message_count BIGINT NOT NULL DEFAULT 0,
    active_days INT NOT NULL DEFAULT 0,
    mention_count BIGINT NOT NULL DEFAULT 0,
    reply_count BIGINT NOT NULL DEFAULT 0,
    replied_by_count BIGINT NOT NULL DEFAULT 0,
    session_count BIGINT NOT NULL DEFAULT 0,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    enabled TINYINT(1) NOT NULL DEFAULT 1,
    created_by VARCHAR(128) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_chat_member_profile_source (source_member_candidate_id),
    KEY idx_chat_member_profile_group (group_id, enabled, status),
    KEY idx_chat_member_profile_member (group_id, sender_uid, sender_uin)
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS chat_knowledge_embedding (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    group_id VARCHAR(64) NOT NULL,
    target_type VARCHAR(32) NOT NULL,
    target_id BIGINT NOT NULL,
    embedding_model VARCHAR(128) NOT NULL,
    embedding_dim INT NOT NULL DEFAULT 0,
    embedding_text TEXT NULL,
    embedding_vector LONGTEXT NULL,
    embedding_hash VARCHAR(128) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'SUCCESS',
    error_message TEXT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_chat_embedding_target (target_type, target_id, embedding_model, status),
    KEY idx_chat_embedding_group_status (group_id, status),
    KEY idx_chat_embedding_hash (embedding_hash)
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS chat_knowledge_publish_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    source_type VARCHAR(32) NOT NULL,
    source_id BIGINT NOT NULL,
    target_type VARCHAR(32) NOT NULL,
    target_id BIGINT NULL,
    action VARCHAR(32) NOT NULL,
    operator VARCHAR(128) NULL,
    comment VARCHAR(512) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_chat_publish_log_source (source_type, source_id),
    KEY idx_chat_publish_log_target (target_type, target_id),
    KEY idx_chat_publish_log_created (created_at)
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
