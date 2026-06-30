package com.yh.qqbot.adapter.dev;

import com.yh.qqbot.dto.AdminOverviewResponse;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile({"dev", "local"})
@RequestMapping("/dev/admin/overview")
public class AdminOverviewController {

    private final JdbcTemplate jdbcTemplate;

    public AdminOverviewController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping
    public AdminOverviewResponse overview(@RequestParam(required = false) String groupId) {
        String normalizedGroupId = hasText(groupId) ? groupId.strip() : null;
        return new AdminOverviewResponse(
                normalizedGroupId,
                Instant.now(),
                count("chat_import_batch", normalizedGroupId, null),
                count("chat_raw_message", normalizedGroupId, null),
                count("chat_clean_message", normalizedGroupId, null),
                count("chat_session", normalizedGroupId, null),
                count("chat_member_stat", normalizedGroupId, null),
                count("chat_knowledge_candidate", normalizedGroupId, null),
                count("chat_knowledge_candidate", normalizedGroupId, "status = 'PENDING'"),
                count("chat_member_candidate", normalizedGroupId, null),
                count("chat_member_candidate", normalizedGroupId, "status = 'PENDING'"),
                count("chat_group_knowledge", normalizedGroupId, "status = 'ACTIVE'"),
                count("chat_group_knowledge", normalizedGroupId, "status = 'ACTIVE' and enabled = 1"),
                count("chat_member_profile", normalizedGroupId, "status = 'ACTIVE'"),
                count("chat_member_profile", normalizedGroupId, "status = 'ACTIVE' and enabled = 1"),
                count("chat_knowledge_embedding", normalizedGroupId, "status = 'SUCCESS'"),
                count("trigger_log", normalizedGroupId, "created_at >= CURDATE()"),
                count("admin_op_log", normalizedGroupId, "created_at >= CURDATE()"),
                latestImport(normalizedGroupId));
    }

    private Long count(String tableName, String groupId, String extraCondition) {
        try {
            Query query = countQuery(tableName, groupId, extraCondition);
            return jdbcTemplate.queryForObject(query.sql(), Long.class, query.args().toArray());
        } catch (Exception ex) {
            return null;
        }
    }

    private AdminOverviewResponse.LatestImport latestImport(String groupId) {
        try {
            Query query = latestImportQuery(groupId);
            List<AdminOverviewResponse.LatestImport> rows = jdbcTemplate.query(
                    query.sql(),
                    (rs, rowNum) -> new AdminOverviewResponse.LatestImport(
                            rs.getLong("id"),
                            rs.getString("status"),
                            rs.getLong("raw_count"),
                            rs.getLong("clean_count"),
                            rs.getLong("session_count"),
                            rs.getLong("member_count"),
                            rs.getString("source_file"),
                            toLocalDateTime(rs.getTimestamp("created_at"))),
                    query.args().toArray());
            return rows.isEmpty() ? null : rows.get(0);
        } catch (Exception ex) {
            return null;
        }
    }

    private Query countQuery(String tableName, String groupId, String extraCondition) {
        StringBuilder sql = new StringBuilder("select count(*) from ").append(tableName);
        List<Object> args = new ArrayList<>();
        appendWhere(sql, args, groupId, extraCondition);
        return new Query(sql.toString(), args);
    }

    private Query latestImportQuery(String groupId) {
        StringBuilder sql = new StringBuilder("""
                select id, status, raw_count, clean_count, session_count, member_count, source_file, created_at
                from chat_import_batch
                """);
        List<Object> args = new ArrayList<>();
        appendWhere(sql, args, groupId, null);
        sql.append(" order by created_at desc limit 1");
        return new Query(sql.toString(), args);
    }

    private void appendWhere(StringBuilder sql, List<Object> args, String groupId, String extraCondition) {
        List<String> conditions = new ArrayList<>();
        if (hasText(groupId)) {
            conditions.add("group_id = ?");
            args.add(groupId);
        }
        if (hasText(extraCondition)) {
            conditions.add(extraCondition);
        }
        if (!conditions.isEmpty()) {
            sql.append(" where ").append(String.join(" and ", conditions));
        }
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private record Query(String sql, List<Object> args) {
    }
}
