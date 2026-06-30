package com.yh.qqbot.chat.history.controller;

import com.yh.qqbot.chat.history.dto.ChatHistoryInsightResponse;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.context.annotation.Profile;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile({"dev", "local"})
@RequestMapping("/dev/chat-history")
public class ChatHistoryInsightController {

    private static final int DEFAULT_TOP_N = 5;
    private static final int MAX_TOP_N = 50;

    private final JdbcTemplate jdbcTemplate;

    public ChatHistoryInsightController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/insights")
    public ChatHistoryInsightResponse insights(
            @RequestParam String groupId,
            @RequestParam(required = false) Long batchId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Integer topN) {
        String normalizedGroupId = groupId == null ? "" : groupId.strip();
        Long resolvedBatchId = batchId == null ? latestSuccessfulBatchId(normalizedGroupId) : batchId;
        int normalizedTopN = normalizeTopN(topN);
        return new ChatHistoryInsightResponse(
                normalizedGroupId,
                resolvedBatchId,
                startDate,
                endDate,
                normalizedTopN,
                summary(normalizedGroupId, resolvedBatchId, startDate, endDate),
                dailyActivities(normalizedGroupId, resolvedBatchId, startDate, endDate),
                topMembers(normalizedGroupId, resolvedBatchId, startDate, endDate, normalizedTopN));
    }

    private Long latestSuccessfulBatchId(String groupId) {
        List<Long> rows = jdbcTemplate.query(
                "select id from chat_import_batch where group_id = ? and status = 'SUCCESS' order by id desc limit 1",
                (rs, rowNum) -> rs.getLong("id"),
                groupId);
        return rows.isEmpty() ? null : rows.get(0);
    }

    private ChatHistoryInsightResponse.Summary summary(
            String groupId,
            Long batchId,
            LocalDate startDate,
            LocalDate endDate) {
        CountQuery statQuery = statSummaryQuery(groupId, batchId, startDate, endDate);
        StatSummary stat = jdbcTemplate.queryForObject(statQuery.sql(), (rs, rowNum) -> new StatSummary(
                rs.getLong("raw_messages"),
                rs.getLong("clean_messages"),
                rs.getLong("mentions"),
                rs.getLong("replies"),
                rs.getLong("replied_by"),
                rs.getLong("members"),
                rs.getLong("active_days")), statQuery.args().toArray());

        CountQuery sessionQuery = sessionCountQuery(groupId, batchId, startDate, endDate);
        Long sessions = jdbcTemplate.queryForObject(sessionQuery.sql(), Long.class, sessionQuery.args().toArray());

        CountQuery timeQuery = messageTimeRangeQuery(groupId, batchId, startDate, endDate);
        TimeRange timeRange = jdbcTemplate.queryForObject(timeQuery.sql(), (rs, rowNum) -> new TimeRange(
                toLocalDateTime(rs.getTimestamp("first_message_at")),
                toLocalDateTime(rs.getTimestamp("last_message_at"))), timeQuery.args().toArray());

        return new ChatHistoryInsightResponse.Summary(
                stat.rawMessages(),
                stat.cleanMessages(),
                stat.mentions(),
                stat.replies(),
                stat.repliedBy(),
                safeLong(sessions),
                stat.members(),
                stat.activeDays(),
                timeRange == null ? null : timeRange.firstMessageAt(),
                timeRange == null ? null : timeRange.lastMessageAt());
    }

    private List<ChatHistoryInsightResponse.DailyActivity> dailyActivities(
            String groupId,
            Long batchId,
            LocalDate startDate,
            LocalDate endDate) {
        CountQuery query = dailyQuery(groupId, batchId, startDate, endDate);
        List<ChatHistoryInsightResponse.DailyActivity> daily = jdbcTemplate.query(
                query.sql(),
                (rs, rowNum) -> new ChatHistoryInsightResponse.DailyActivity(
                        toLocalDate(rs.getDate("stat_date")),
                        rs.getLong("raw_messages"),
                        rs.getLong("clean_messages"),
                        rs.getLong("mentions"),
                        rs.getLong("replies"),
                        rs.getLong("replied_by"),
                        0L,
                        rs.getLong("active_members")),
                query.args().toArray());
        Map<LocalDate, Long> sessionsByDate = sessionsByDate(groupId, batchId, startDate, endDate);
        return daily.stream()
                .map(item -> new ChatHistoryInsightResponse.DailyActivity(
                        item.statDate(),
                        item.rawMessages(),
                        item.cleanMessages(),
                        item.mentions(),
                        item.replies(),
                        item.repliedBy(),
                        sessionsByDate.getOrDefault(item.statDate(), 0L),
                        item.activeMembers()))
                .toList();
    }

    private Map<LocalDate, Long> sessionsByDate(String groupId, Long batchId, LocalDate startDate, LocalDate endDate) {
        CountQuery query = sessionsByDateQuery(groupId, batchId, startDate, endDate);
        Map<LocalDate, Long> result = new LinkedHashMap<>();
        jdbcTemplate.query(query.sql(), rs -> {
            result.put(toLocalDate(rs.getDate("stat_date")), rs.getLong("sessions"));
        }, query.args().toArray());
        return result;
    }

    private List<ChatHistoryInsightResponse.MemberDigest> topMembers(
            String groupId,
            Long batchId,
            LocalDate startDate,
            LocalDate endDate,
            int topN) {
        CountQuery query = topMembersQuery(groupId, batchId, startDate, endDate, topN);
        return jdbcTemplate.query(
                query.sql(),
                (rs, rowNum) -> new ChatHistoryInsightResponse.MemberDigest(
                        rs.getString("sender_uid"),
                        rs.getString("sender_uin"),
                        rs.getString("sender_name"),
                        rs.getLong("raw_messages"),
                        rs.getLong("clean_messages"),
                        rs.getLong("mentions"),
                        rs.getLong("replies"),
                        rs.getLong("replied_by"),
                        rs.getLong("sessions")),
                query.args().toArray());
    }

    private CountQuery statSummaryQuery(String groupId, Long batchId, LocalDate startDate, LocalDate endDate) {
        StringBuilder sql = new StringBuilder("""
                select
                  coalesce(sum(raw_message_count), 0) raw_messages,
                  coalesce(sum(message_count), 0) clean_messages,
                  coalesce(sum(mention_count), 0) mentions,
                  coalesce(sum(reply_count), 0) replies,
                  coalesce(sum(replied_by_count), 0) replied_by,
                  count(distinct coalesce(sender_uid, sender_uin, sender_name, 'unknown')) members,
                  count(distinct stat_date) active_days
                from chat_member_stat_daily
                """);
        List<Object> args = new ArrayList<>();
        appendDailyWhere(sql, args, groupId, batchId, startDate, endDate);
        return new CountQuery(sql.toString(), args);
    }

    private CountQuery sessionCountQuery(String groupId, Long batchId, LocalDate startDate, LocalDate endDate) {
        StringBuilder sql = new StringBuilder("select count(*) from chat_session");
        List<Object> args = new ArrayList<>();
        appendSessionWhere(sql, args, groupId, batchId, startDate, endDate);
        return new CountQuery(sql.toString(), args);
    }

    private CountQuery messageTimeRangeQuery(String groupId, Long batchId, LocalDate startDate, LocalDate endDate) {
        StringBuilder sql = new StringBuilder("""
                select min(message_time) first_message_at, max(message_time) last_message_at
                from chat_clean_message
                """);
        List<Object> args = new ArrayList<>();
        appendMessageWhere(sql, args, groupId, batchId, startDate, endDate);
        return new CountQuery(sql.toString(), args);
    }

    private CountQuery dailyQuery(String groupId, Long batchId, LocalDate startDate, LocalDate endDate) {
        StringBuilder sql = new StringBuilder("""
                select
                  stat_date,
                  coalesce(sum(raw_message_count), 0) raw_messages,
                  coalesce(sum(message_count), 0) clean_messages,
                  coalesce(sum(mention_count), 0) mentions,
                  coalesce(sum(reply_count), 0) replies,
                  coalesce(sum(replied_by_count), 0) replied_by,
                  count(distinct coalesce(sender_uid, sender_uin, sender_name, 'unknown')) active_members
                from chat_member_stat_daily
                """);
        List<Object> args = new ArrayList<>();
        appendDailyWhere(sql, args, groupId, batchId, startDate, endDate);
        sql.append(" group by stat_date order by stat_date asc");
        return new CountQuery(sql.toString(), args);
    }

    private CountQuery sessionsByDateQuery(String groupId, Long batchId, LocalDate startDate, LocalDate endDate) {
        StringBuilder sql = new StringBuilder("""
                select date(start_time) stat_date, count(*) sessions
                from chat_session
                """);
        List<Object> args = new ArrayList<>();
        appendSessionWhere(sql, args, groupId, batchId, startDate, endDate);
        sql.append(" group by date(start_time) order by date(start_time) asc");
        return new CountQuery(sql.toString(), args);
    }

    private CountQuery topMembersQuery(
            String groupId,
            Long batchId,
            LocalDate startDate,
            LocalDate endDate,
            int topN) {
        StringBuilder sql = new StringBuilder("""
                select
                  sender_uid,
                  sender_uin,
                  sender_name,
                  coalesce(sum(raw_message_count), 0) raw_messages,
                  coalesce(sum(message_count), 0) clean_messages,
                  coalesce(sum(mention_count), 0) mentions,
                  coalesce(sum(reply_count), 0) replies,
                  coalesce(sum(replied_by_count), 0) replied_by,
                  coalesce(sum(session_count), 0) sessions
                from chat_member_stat_daily
                """);
        List<Object> args = new ArrayList<>();
        appendDailyWhere(sql, args, groupId, batchId, startDate, endDate);
        sql.append("""
                 group by sender_uid, sender_uin, sender_name
                 having clean_messages > 0
                 order by clean_messages desc, sender_name asc
                 limit
                """).append(topN);
        return new CountQuery(sql.toString(), args);
    }

    private void appendDailyWhere(
            StringBuilder sql,
            List<Object> args,
            String groupId,
            Long batchId,
            LocalDate startDate,
            LocalDate endDate) {
        List<String> conditions = new ArrayList<>();
        conditions.add("group_id = ?");
        args.add(groupId);
        if (batchId != null) {
            conditions.add("batch_id = ?");
            args.add(batchId);
        }
        if (startDate != null) {
            conditions.add("stat_date >= ?");
            args.add(startDate);
        }
        if (endDate != null) {
            conditions.add("stat_date <= ?");
            args.add(endDate);
        }
        sql.append(" where ").append(String.join(" and ", conditions));
    }

    private void appendSessionWhere(
            StringBuilder sql,
            List<Object> args,
            String groupId,
            Long batchId,
            LocalDate startDate,
            LocalDate endDate) {
        List<String> conditions = new ArrayList<>();
        conditions.add("group_id = ?");
        args.add(groupId);
        if (batchId != null) {
            conditions.add("batch_id = ?");
            args.add(batchId);
        }
        if (startDate != null) {
            conditions.add("date(start_time) >= ?");
            args.add(startDate);
        }
        if (endDate != null) {
            conditions.add("date(start_time) <= ?");
            args.add(endDate);
        }
        sql.append(" where ").append(String.join(" and ", conditions));
    }

    private void appendMessageWhere(
            StringBuilder sql,
            List<Object> args,
            String groupId,
            Long batchId,
            LocalDate startDate,
            LocalDate endDate) {
        List<String> conditions = new ArrayList<>();
        conditions.add("group_id = ?");
        args.add(groupId);
        if (batchId != null) {
            conditions.add("batch_id = ?");
            args.add(batchId);
        }
        if (startDate != null) {
            conditions.add("date(message_time) >= ?");
            args.add(startDate);
        }
        if (endDate != null) {
            conditions.add("date(message_time) <= ?");
            args.add(endDate);
        }
        sql.append(" where ").append(String.join(" and ", conditions));
    }

    private int normalizeTopN(Integer topN) {
        if (topN == null || topN <= 0) {
            return DEFAULT_TOP_N;
        }
        return Math.min(topN, MAX_TOP_N);
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private LocalDate toLocalDate(Date date) {
        return date == null ? null : date.toLocalDate();
    }

    private Long safeLong(Long value) {
        return value == null ? 0L : value;
    }

    private record CountQuery(String sql, List<Object> args) {
    }

    private record StatSummary(
            Long rawMessages,
            Long cleanMessages,
            Long mentions,
            Long replies,
            Long repliedBy,
            Long members,
            Long activeDays
    ) {
    }

    private record TimeRange(LocalDateTime firstMessageAt, LocalDateTime lastMessageAt) {
    }
}
