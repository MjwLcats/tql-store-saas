package com.tql.store.integration.service;

import com.tql.store.common.api.PageResult;
import com.tql.store.integration.model.SyncLogView;
import com.tql.store.integration.model.SyncTaskCreateRequest;
import com.tql.store.integration.model.SyncTaskView;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class SyncTaskService {

    private static final Set<String> PROVIDERS = Set.of("HUALALA", "HR_BUTLER");
    private static final Set<String> SYNC_MODES = Set.of("INCREMENTAL", "FULL");
    private static final Set<String> STATUSES = Set.of("PENDING", "RUNNING", "SUCCESS", "FAILED");
    private static final Map<String, Set<String>> PROVIDER_DATA_TYPES = Map.of(
            "HUALALA", Set.of("SHOP", "BILL", "DISH_SALES"),
            "HR_BUTLER", Set.of("ORGANIZATION", "POSITION", "USER")
    );

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final SyncTaskExecutor taskExecutor;

    public SyncTaskService(NamedParameterJdbcTemplate jdbcTemplate, SyncTaskExecutor taskExecutor) {
        this.jdbcTemplate = jdbcTemplate;
        this.taskExecutor = taskExecutor;
    }

    public PageResult<SyncTaskView> list(
            Long tenantId, String provider, String dataType, String status, int page, int pageSize) {
        int safePage = Math.max(page, 1);
        int safePageSize = Math.min(Math.max(pageSize, 1), 100);
        StringBuilder where = new StringBuilder(" WHERE t.tenant_id = :tenantId ");
        MapSqlParameterSource params = new MapSqlParameterSource("tenantId", tenantId);

        appendFilter(where, params, "provider", provider, "t.provider");
        appendFilter(where, params, "dataType", dataType, "t.data_type");
        appendFilter(where, params, "status", status, "t.status");

        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM integration_sync_task t" + where, params, Long.class);
        params.addValue("offset", (safePage - 1) * safePageSize);
        params.addValue("pageSize", safePageSize);

        List<SyncTaskView> records = jdbcTemplate.query("""
                SELECT t.id, t.provider, t.data_type, t.sync_mode, t.trigger_type, t.retry_of,
                       t.range_start, t.range_end, t.status, t.total_count, t.success_count,
                       t.failed_count, t.error_message, t.created_by,
                       COALESCE(u.display_name, u.username, '系统任务') AS creator_name,
                       t.started_at, t.finished_at, t.create_time
                FROM integration_sync_task t
                LEFT JOIN sys_merchant_user u ON u.id = t.created_by AND u.tenant_id = t.tenant_id
                """ + where + " ORDER BY t.id DESC LIMIT :offset, :pageSize", params,
                (rs, rowNum) -> new SyncTaskView(
                        rs.getLong("id"),
                        rs.getString("provider"),
                        rs.getString("data_type"),
                        rs.getString("sync_mode"),
                        rs.getString("trigger_type"),
                        rs.getObject("retry_of", Long.class),
                        rs.getDate("range_start") == null ? null : rs.getDate("range_start").toLocalDate(),
                        rs.getDate("range_end") == null ? null : rs.getDate("range_end").toLocalDate(),
                        rs.getString("status"),
                        rs.getInt("total_count"),
                        rs.getInt("success_count"),
                        rs.getInt("failed_count"),
                        rs.getString("error_message"),
                        rs.getLong("created_by"),
                        rs.getString("creator_name"),
                        toLocalDateTime(rs.getTimestamp("started_at")),
                        toLocalDateTime(rs.getTimestamp("finished_at")),
                        toLocalDateTime(rs.getTimestamp("create_time"))
                ));
        return new PageResult<>(records, total == null ? 0 : total, safePage, safePageSize);
    }

    public Long create(SyncTaskCreateRequest request, Long tenantId, Long userId) {
        NormalizedRequest normalized = normalize(request);
        return createTask(normalized, tenantId, userId, "MANUAL", null);
    }

    public Long retry(Long originalTaskId, Long tenantId, Long userId) {
        List<NormalizedRequest> tasks = jdbcTemplate.query("""
                SELECT provider, data_type, sync_mode, range_start, range_end
                FROM integration_sync_task
                WHERE id = :id AND tenant_id = :tenantId AND status = 'FAILED'
                """, new MapSqlParameterSource("id", originalTaskId).addValue("tenantId", tenantId),
                (rs, rowNum) -> new NormalizedRequest(
                        rs.getString("provider"),
                        rs.getString("data_type"),
                        rs.getString("sync_mode"),
                        rs.getDate("range_start") == null ? null : rs.getDate("range_start").toLocalDate(),
                        rs.getDate("range_end") == null ? null : rs.getDate("range_end").toLocalDate()
                ));
        if (tasks.isEmpty()) {
            throw new IllegalArgumentException("仅失败的同步任务可以重试");
        }
        return createTask(tasks.get(0), tenantId, userId, "RETRY", originalTaskId);
    }

    public List<SyncLogView> logs(Long taskId, Long tenantId) {
        Long count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM integration_sync_task WHERE id = :id AND tenant_id = :tenantId
                """, new MapSqlParameterSource("id", taskId).addValue("tenantId", tenantId), Long.class);
        if (count == null || count == 0) {
            throw new IllegalArgumentException("同步任务不存在");
        }
        return jdbcTemplate.query("""
                SELECT id, log_level, stage, message, detail, create_time
                FROM integration_sync_log
                WHERE task_id = :taskId AND tenant_id = :tenantId
                ORDER BY id
                """, new MapSqlParameterSource("taskId", taskId).addValue("tenantId", tenantId),
                (rs, rowNum) -> new SyncLogView(
                        rs.getLong("id"),
                        rs.getString("log_level"),
                        rs.getString("stage"),
                        rs.getString("message"),
                        rs.getString("detail"),
                        toLocalDateTime(rs.getTimestamp("create_time"))
                ));
    }

    @EventListener(ApplicationReadyEvent.class)
    public void recoverInterruptedTasks() {
        List<InterruptedTask> tasks = jdbcTemplate.query("""
                SELECT id, tenant_id FROM integration_sync_task
                WHERE status IN ('PENDING', 'RUNNING')
                """, new MapSqlParameterSource(),
                (rs, rowNum) -> new InterruptedTask(rs.getLong("id"), rs.getLong("tenant_id")));
        for (InterruptedTask task : tasks) {
            jdbcTemplate.update("""
                    UPDATE integration_sync_task
                    SET status = 'FAILED', error_message = '服务重启导致任务中断',
                        active_lock_key = NULL, finished_at = NOW()
                    WHERE id = :id AND tenant_id = :tenantId AND status IN ('PENDING', 'RUNNING')
                    """, new MapSqlParameterSource("id", task.id()).addValue("tenantId", task.tenantId()));
            addLog(task.tenantId(), task.id(), "ERROR", "INTERRUPTED", "服务重启导致任务中断", null);
        }
    }

    private Long createTask(
            NormalizedRequest request, Long tenantId, Long userId, String triggerType, Long retryOf) {
        String lockKey = tenantId + ":" + request.provider() + ":" + request.dataType();
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("tenantId", tenantId)
                .addValue("provider", request.provider())
                .addValue("dataType", request.dataType())
                .addValue("syncMode", request.syncMode())
                .addValue("triggerType", triggerType)
                .addValue("retryOf", retryOf)
                .addValue("rangeStart", request.rangeStart())
                .addValue("rangeEnd", request.rangeEnd())
                .addValue("lockKey", lockKey)
                .addValue("createdBy", userId);
        KeyHolder keyHolder = new GeneratedKeyHolder();
        try {
            jdbcTemplate.update("""
                    INSERT INTO integration_sync_task
                        (tenant_id, provider, data_type, sync_mode, trigger_type, retry_of,
                         range_start, range_end, status, active_lock_key, created_by)
                    VALUES
                        (:tenantId, :provider, :dataType, :syncMode, :triggerType, :retryOf,
                         :rangeStart, :rangeEnd, 'PENDING', :lockKey, :createdBy)
                    """, params, keyHolder, new String[]{"id"});
        } catch (DuplicateKeyException ex) {
            throw new IllegalArgumentException("同类型同步任务正在执行，请稍后再试");
        }
        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("同步任务创建失败");
        }
        Long taskId = key.longValue();
        addLog(tenantId, taskId, "INFO", "QUEUED",
                "同步任务已进入执行队列", retryOf == null ? null : "retryOf=" + retryOf);
        taskExecutor.execute(taskId, tenantId);
        return taskId;
    }

    private NormalizedRequest normalize(SyncTaskCreateRequest request) {
        String provider = normalizeValue(request.provider());
        String dataType = normalizeValue(request.dataType());
        String syncMode = normalizeValue(request.syncMode());
        if (!PROVIDERS.contains(provider)) {
            throw new IllegalArgumentException("不支持的数据来源");
        }
        if (!PROVIDER_DATA_TYPES.get(provider).contains(dataType)) {
            throw new IllegalArgumentException("数据来源与数据类型不匹配");
        }
        if (!SYNC_MODES.contains(syncMode)) {
            throw new IllegalArgumentException("不支持的同步方式");
        }
        LocalDate rangeStart = request.rangeStart();
        LocalDate rangeEnd = request.rangeEnd();
        if ("HUALALA".equals(provider) && !"SHOP".equals(dataType)) {
            if (rangeStart == null || rangeEnd == null) {
                throw new IllegalArgumentException("请选择同步日期范围");
            }
            if (rangeEnd.isBefore(rangeStart)) {
                throw new IllegalArgumentException("结束日期不能早于开始日期");
            }
            if (ChronoUnit.DAYS.between(rangeStart, rangeEnd) > 30) {
                throw new IllegalArgumentException("单次同步最多支持31天");
            }
        } else {
            rangeStart = null;
            rangeEnd = null;
        }
        return new NormalizedRequest(provider, dataType, syncMode, rangeStart, rangeEnd);
    }

    private void appendFilter(
            StringBuilder where, MapSqlParameterSource params, String name, String value, String column) {
        if (value != null && !value.isBlank()) {
            String normalized = normalizeValue(value);
            if ("status".equals(name) && !STATUSES.contains(normalized)) {
                throw new IllegalArgumentException("同步状态不正确");
            }
            where.append(" AND ").append(column).append(" = :").append(name).append(' ');
            params.addValue(name, normalized);
        }
    }

    private void addLog(
            Long tenantId, Long taskId, String level, String stage, String message, String detail) {
        jdbcTemplate.update("""
                INSERT INTO integration_sync_log
                    (tenant_id, task_id, log_level, stage, message, detail)
                VALUES (:tenantId, :taskId, :level, :stage, :message, :detail)
                """, new MapSqlParameterSource("tenantId", tenantId)
                .addValue("taskId", taskId)
                .addValue("level", level)
                .addValue("stage", stage)
                .addValue("message", message)
                .addValue("detail", detail));
    }

    private String normalizeValue(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private java.time.LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private record NormalizedRequest(
            String provider, String dataType, String syncMode, LocalDate rangeStart, LocalDate rangeEnd) {
    }

    private record InterruptedTask(Long id, Long tenantId) {
    }
}
