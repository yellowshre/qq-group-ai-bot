package com.yh.qqbot.config;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Profile({"dev", "local"})
@Order(Ordered.HIGHEST_PRECEDENCE)
public class DevLocalSchemaMigrationRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DevLocalSchemaMigrationRunner.class);
    private static final String GROUP_CONFIG_TABLE = "group_config";

    private final JdbcTemplate jdbcTemplate;

    public DevLocalSchemaMigrationRunner(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            ensureGroupConfigColumns();
        } catch (Exception ex) {
            log.warn("Dev/local schema migration skipped. reason={}", ex.getMessage(), ex);
        }
    }

    void ensureGroupConfigColumns() {
        if (!tableExists(GROUP_CONFIG_TABLE)) {
            log.warn("Dev/local schema migration skipped. table={} not found", GROUP_CONFIG_TABLE);
            return;
        }
        int added = 0;
        for (ColumnPatch patch : groupConfigPatches()) {
            if (columnExists(GROUP_CONFIG_TABLE, patch.columnName())) {
                continue;
            }
            jdbcTemplate.execute(patch.alterSql());
            added++;
            log.info("Dev/local schema migration applied. table={}, column={}", GROUP_CONFIG_TABLE, patch.columnName());
        }
        log.info("Dev/local schema migration completed. table={}, addedColumns={}", GROUP_CONFIG_TABLE, added);
    }

    private boolean tableExists(String tableName) {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.TABLES
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = ?
                """,
                Integer.class,
                tableName);
        return count != null && count > 0;
    }

    private boolean columnExists(String tableName, String columnName) {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = ?
                  AND COLUMN_NAME = ?
                """,
                Integer.class,
                tableName,
                columnName);
        return count != null && count > 0;
    }

    private List<ColumnPatch> groupConfigPatches() {
        return List.of(
                new ColumnPatch(
                        "enable_meme",
                        "ALTER TABLE group_config ADD COLUMN enable_meme TINYINT(1) NOT NULL DEFAULT 1 AFTER enable_chat"),
                new ColumnPatch(
                        "enable_passive_chat",
                        "ALTER TABLE group_config ADD COLUMN enable_passive_chat TINYINT(1) NOT NULL DEFAULT 1 AFTER enable_meme"),
                new ColumnPatch(
                        "active_cooldown_seconds",
                        "ALTER TABLE group_config ADD COLUMN active_cooldown_seconds BIGINT NOT NULL DEFAULT 180 AFTER enable_auto_join"),
                new ColumnPatch(
                        "active_hour_limit",
                        "ALTER TABLE group_config ADD COLUMN active_hour_limit BIGINT NOT NULL DEFAULT 20 AFTER active_cooldown_seconds"),
                new ColumnPatch(
                        "active_day_limit",
                        "ALTER TABLE group_config ADD COLUMN active_day_limit BIGINT NOT NULL DEFAULT 80 AFTER active_hour_limit"),
                new ColumnPatch(
                        "safe_word",
                        "ALTER TABLE group_config ADD COLUMN safe_word VARCHAR(128) NULL AFTER active_day_limit"),
                new ColumnPatch(
                        "safe_word_reply",
                        "ALTER TABLE group_config ADD COLUMN safe_word_reply VARCHAR(512) NOT NULL DEFAULT '收到，我先安静一下。' AFTER safe_word"),
                new ColumnPatch(
                        "persona",
                        "ALTER TABLE group_config ADD COLUMN persona VARCHAR(1024) NOT NULL DEFAULT '' AFTER safe_word_reply"),
                new ColumnPatch(
                        "memory_mode",
                        "ALTER TABLE group_config ADD COLUMN memory_mode VARCHAR(16) NOT NULL DEFAULT 'SHORT' AFTER persona"),
                new ColumnPatch(
                        "enable_knowledge_context",
                        "ALTER TABLE group_config ADD COLUMN enable_knowledge_context TINYINT(1) NOT NULL DEFAULT 0 AFTER memory_mode"),
                new ColumnPatch(
                        "enable_meme_knowledge",
                        "ALTER TABLE group_config ADD COLUMN enable_meme_knowledge TINYINT(1) NOT NULL DEFAULT 0 AFTER enable_knowledge_context"),
                new ColumnPatch(
                        "enable_passive_chat_knowledge",
                        "ALTER TABLE group_config ADD COLUMN enable_passive_chat_knowledge TINYINT(1) NOT NULL DEFAULT 0 AFTER enable_meme_knowledge"),
                new ColumnPatch(
                        "enable_active_chat_knowledge",
                        "ALTER TABLE group_config ADD COLUMN enable_active_chat_knowledge TINYINT(1) NOT NULL DEFAULT 0 AFTER enable_passive_chat_knowledge"));
    }

    private record ColumnPatch(String columnName, String alterSql) {
    }
}
