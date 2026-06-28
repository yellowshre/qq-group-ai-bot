SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

CREATE TABLE IF NOT EXISTS chat_knowledge_candidate (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    batch_id BIGINT NOT NULL,
    group_id VARCHAR(64) NOT NULL,
    candidate_type VARCHAR(32) NOT NULL,
    title VARCHAR(100) NOT NULL,
    content TEXT NOT NULL,
    source_session_id BIGINT NULL,
    source_message_ids TEXT NULL,
    evidence_text TEXT NULL,
    hit_count BIGINT NOT NULL DEFAULT 0,
    member_count BIGINT NOT NULL DEFAULT 0,
    confidence DECIMAL(8,4) NOT NULL DEFAULT 0,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    reviewer VARCHAR(128) NULL,
    review_comment VARCHAR(512) NULL,
    reviewed_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_chat_knowledge_candidate_query (batch_id, group_id, status, candidate_type),
    KEY idx_chat_knowledge_candidate_reviewed (status, reviewed_at)
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS chat_member_candidate (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    batch_id BIGINT NOT NULL,
    group_id VARCHAR(64) NOT NULL,
    sender_uid VARCHAR(128) NULL,
    sender_uin VARCHAR(64) NULL,
    sender_name VARCHAR(255) NULL,
    message_count BIGINT NOT NULL DEFAULT 0,
    raw_message_count BIGINT NOT NULL DEFAULT 0,
    active_days INT NOT NULL DEFAULT 0,
    mention_count BIGINT NOT NULL DEFAULT 0,
    reply_count BIGINT NOT NULL DEFAULT 0,
    replied_by_count BIGINT NOT NULL DEFAULT 0,
    session_count BIGINT NOT NULL DEFAULT 0,
    score BIGINT NOT NULL DEFAULT 0,
    candidate_reason VARCHAR(512) NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    reviewer VARCHAR(128) NULL,
    review_comment VARCHAR(512) NULL,
    reviewed_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_chat_member_candidate_query (batch_id, group_id, status, score),
    KEY idx_chat_member_candidate_member (batch_id, group_id, sender_uid, sender_uin)
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS chat_knowledge_review_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    target_type VARCHAR(32) NOT NULL,
    target_id BIGINT NOT NULL,
    old_status VARCHAR(32) NULL,
    new_status VARCHAR(32) NOT NULL,
    reviewer VARCHAR(128) NULL,
    review_comment VARCHAR(512) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_chat_review_log_target (target_type, target_id),
    KEY idx_chat_review_log_created (created_at)
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
