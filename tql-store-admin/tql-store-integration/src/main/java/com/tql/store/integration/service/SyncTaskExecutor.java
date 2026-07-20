package com.tql.store.integration.service;

import com.tql.store.integration.provider.SyncExecutionContext;
import com.tql.store.integration.provider.SyncExecutionResult;
import com.tql.store.integration.provider.ThirdPartySyncAdapter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class SyncTaskExecutor {

    private final JdbcTemplate jdbcTemplate;
    private final Map<String, ThirdPartySyncAdapter> adapters;

    public SyncTaskExecutor(JdbcTemplate jdbcTemplate, List<ThirdPartySyncAdapter> adapters) {
        this.jdbcTemplate = jdbcTemplate;
        this.adapters = adapters.stream().collect(Collectors.toUnmodifiableMap(
                adapter -> adapter.provider().toUpperCase(Locale.ROOT), Function.identity()));
    }

    @Async("integrationTaskExecutor")
    public void execute(Long taskId, Long tenantId) {
        int changed = jdbcTemplate.update("""
                UPDATE integration_sync_task
                SET status = 'RUNNING', started_at = NOW(), error_message = NULL
                WHERE id = ? AND tenant_id = ? AND status = 'PENDING'
                """, taskId, tenantId);
        if (changed == 0) {
            return;
        }

        addLog(tenantId, taskId, "INFO", "STARTED", "同步任务开始执行", null);
        try {
            SyncExecutionContext context = jdbcTemplate.queryForObject("""
                    SELECT id, tenant_id, provider, data_type, sync_mode, range_start, range_end
                    FROM integration_sync_task
                    WHERE id = ? AND tenant_id = ?
                    """, (rs, rowNum) -> new SyncExecutionContext(
                    rs.getLong("id"),
                    rs.getLong("tenant_id"),
                    rs.getString("provider"),
                    rs.getString("data_type"),
                    rs.getString("sync_mode"),
                    rs.getDate("range_start") == null ? null : rs.getDate("range_start").toLocalDate(),
                    rs.getDate("range_end") == null ? null : rs.getDate("range_end").toLocalDate()
            ), taskId, tenantId);
            if (context == null) {
                throw new IllegalStateException("同步任务不存在");
            }

            ThirdPartySyncAdapter adapter = adapters.get(context.provider().toUpperCase(Locale.ROOT));
            if (adapter == null) {
                throw new IllegalStateException("未找到第三方数据适配器");
            }
            addLog(tenantId, taskId, "INFO", "CONNECTING",
                    "准备连接第三方数据源", "provider=" + context.provider());
            SyncExecutionResult result = adapter.sync(context);

            jdbcTemplate.update("""
                    UPDATE integration_sync_task
                    SET status = 'SUCCESS', total_count = ?, success_count = ?, failed_count = ?,
                        error_message = NULL, active_lock_key = NULL, finished_at = NOW()
                    WHERE id = ? AND tenant_id = ?
                    """, result.totalCount(), result.successCount(), result.failedCount(), taskId, tenantId);
            addLog(tenantId, taskId, "INFO", "COMPLETED", "同步任务执行成功",
                    "total=" + result.totalCount() + ", success=" + result.successCount()
                            + ", failed=" + result.failedCount());
        } catch (Exception ex) {
            String message = safeMessage(ex);
            jdbcTemplate.update("""
                    UPDATE integration_sync_task
                    SET status = 'FAILED', error_message = ?, active_lock_key = NULL, finished_at = NOW()
                    WHERE id = ? AND tenant_id = ?
                    """, message, taskId, tenantId);
            addLog(tenantId, taskId, "ERROR", "FAILED", message, ex.getClass().getSimpleName());
        }
    }

    private void addLog(
            Long tenantId, Long taskId, String level, String stage, String message, String detail) {
        jdbcTemplate.update("""
                INSERT INTO integration_sync_log
                    (tenant_id, task_id, log_level, stage, message, detail)
                VALUES (?, ?, ?, ?, ?, ?)
                """, tenantId, taskId, level, stage, message, detail);
    }

    private String safeMessage(Exception ex) {
        String message = ex.getMessage();
        if (message == null || message.isBlank()) {
            message = "第三方数据同步失败";
        }
        return message.length() > 1000 ? message.substring(0, 1000) : message;
    }
}
