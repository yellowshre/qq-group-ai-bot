package com.yh.qqbot.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

class DevLocalSchemaMigrationRunnerReflectionTest {

    @Test
    void addsMissingKnowledgeColumns() throws Exception {
        FakeJdbcTemplate jdbcTemplate = new FakeJdbcTemplate(true, Set.of(
                "enable_knowledge_context",
                "enable_meme_knowledge",
                "enable_passive_chat_knowledge",
                "enable_active_chat_knowledge"));
        Object runner = newRunner(jdbcTemplate);

        invoke(runner, "ensureGroupConfigColumns");

        assertThat(jdbcTemplate.executedSql).hasSize(4);
        assertThat(jdbcTemplate.executedSql).anySatisfy(sql -> assertThat(sql).contains("enable_knowledge_context"));
        assertThat(jdbcTemplate.executedSql).anySatisfy(sql -> assertThat(sql).contains("enable_meme_knowledge"));
        assertThat(jdbcTemplate.executedSql).anySatisfy(sql -> assertThat(sql).contains("enable_passive_chat_knowledge"));
        assertThat(jdbcTemplate.executedSql).anySatisfy(sql -> assertThat(sql).contains("enable_active_chat_knowledge"));
    }

    @Test
    void skipsWhenGroupConfigTableDoesNotExist() throws Exception {
        FakeJdbcTemplate jdbcTemplate = new FakeJdbcTemplate(false, Set.of("enable_knowledge_context"));
        Object runner = newRunner(jdbcTemplate);

        invoke(runner, "ensureGroupConfigColumns");

        assertThat(jdbcTemplate.executedSql).isEmpty();
    }

    private Object newRunner(JdbcTemplate jdbcTemplate) throws Exception {
        Class<?> type = Class.forName("com.yh.qqbot.config.DevLocalSchemaMigrationRunner");
        Constructor<?> constructor = type.getConstructor(JdbcTemplate.class);
        return constructor.newInstance(jdbcTemplate);
    }

    private Object invoke(Object target, String methodName, Object... args) throws Exception {
        Method method = target.getClass().getDeclaredMethod(methodName);
        method.setAccessible(true);
        return method.invoke(target, args);
    }

    private static final class FakeJdbcTemplate extends JdbcTemplate {
        private final boolean tableExists;
        private final Set<String> missingColumns;
        private final List<String> executedSql = new ArrayList<>();

        private FakeJdbcTemplate(boolean tableExists, Set<String> missingColumns) {
            this.tableExists = tableExists;
            this.missingColumns = missingColumns;
        }

        @Override
        public <T> T queryForObject(String sql, Class<T> requiredType, Object... args) {
            int count;
            if (sql.contains("information_schema.TABLES")) {
                count = tableExists ? 1 : 0;
            } else if (sql.contains("information_schema.COLUMNS")) {
                String columnName = String.valueOf(args[1]);
                count = missingColumns.contains(columnName) ? 0 : 1;
            } else {
                count = 0;
            }
            return requiredType.cast(count);
        }

        @Override
        public void execute(String sql) {
            executedSql.add(sql);
        }
    }
}
