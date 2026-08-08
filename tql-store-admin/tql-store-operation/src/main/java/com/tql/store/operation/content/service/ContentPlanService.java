package com.tql.store.operation.content.service;

import com.tql.store.operation.content.model.CreateActivityRequest;
import com.tql.store.operation.content.model.CreatePlanRequest;
import com.tql.store.operation.content.model.ContentDeliveryView;
import com.tql.store.operation.content.model.ActivitySummaryView;
import com.tql.store.operation.content.model.EmployeeContentTaskView;
import com.tql.store.operation.content.model.PlanSummaryView;
import com.tql.store.operation.content.model.PlanPrecheckView;
import com.tql.store.operation.content.model.PlanPublishView;
import com.tql.store.operation.content.model.UpdateContentPlanRequest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ContentPlanService {

    private static final Set<String> CREATION_MODES =
            Set.of("STANDARD_TEMPLATE", "AI_ASSISTED", "SELF_CREATED");
    private static final Set<String> TRAINING_POLICIES = Set.of("NONE", "REQUIRED", "DYNAMIC");
    private static final Set<String> PUBLISH_PLATFORMS = Set.of("抖音", "快手", "小红书", "视频号");

    private final JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate namedJdbcTemplate;

    public ContentPlanService(JdbcTemplate jdbcTemplate, NamedParameterJdbcTemplate namedJdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.namedJdbcTemplate = namedJdbcTemplate;
    }

    @Transactional
    public Long createActivity(Long tenantId, Long operatorId, CreateActivityRequest request) {
        validateSchedule(request.startTime(), request.releaseStartTime());
        if (!request.endTime().isAfter(request.startTime())) {
            throw new IllegalArgumentException("活动结束时间必须晚于开始时间");
        }
        Long ownerId = request.ownerId() == null ? operatorId : request.ownerId();
        requireActiveEmployee(tenantId, ownerId);
        return insertAndReturnId("""
                INSERT INTO ops_content_activity
                    (tenant_id, activity_name, objective, start_time, release_start_time, end_time, owner_id, status,
                     create_by, update_by)
                VALUES (?, ?, ?, ?, ?, ?, ?, 'DRAFT', ?, ?)
                """, tenantId, request.name().trim(), trimToNull(request.objective()),
                request.startTime(), request.releaseStartTime(), request.endTime(), ownerId, operatorId, operatorId);
    }

    @Transactional
    public Long createPlan(Long tenantId, Long operatorId, CreatePlanRequest request) {
        validatePlatforms(request.platforms());
        String creationMode = normalized(request.creationMode());
        String trainingPolicy = normalized(request.trainingPolicy());
        int storyboardCount = normalizeStoryboardCount(request.storyboardCount(), creationMode);
        if (!CREATION_MODES.contains(creationMode)) {
            throw new IllegalArgumentException("不支持的创作模式");
        }
        if (!TRAINING_POLICIES.contains(trainingPolicy)) {
            throw new IllegalArgumentException("不支持的前置训练策略");
        }
        LocalDateTime activityEnd = jdbcTemplate.query("""
                SELECT end_time
                FROM ops_content_activity
                WHERE id = ? AND tenant_id = ? AND deleted = 0 AND status = 'DRAFT'
                """, rs -> rs.next() ? rs.getTimestamp("end_time").toLocalDateTime() : null,
                request.activityId(), tenantId);
        if (activityEnd == null) {
            throw new IllegalArgumentException("活动不存在或当前状态不允许创建计划");
        }
        if (request.deadline().isAfter(activityEnd)) {
            throw new IllegalArgumentException("任务截止时间不能晚于活动结束时间");
        }
        return insertAndReturnId("""
                INSERT INTO ops_content_plan
                    (tenant_id, activity_id, plan_name, task_instruction, creation_mode, storyboard_count,
                     training_policy, deadline, status, create_by, update_by)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'DRAFT', ?, ?)
                """, tenantId, request.activityId(), request.name().trim(),
                request.taskInstruction().trim(), creationMode, storyboardCount, trainingPolicy,
                request.deadline(), operatorId, operatorId);
    }

    @Transactional
    public void updateActivity(Long tenantId, Long operatorId, Long activityId, UpdateContentPlanRequest request) {
        validatePlatforms(request.platforms());
        if (!request.endTime().isAfter(request.startTime())) {
            throw new IllegalArgumentException("活动结束时间必须晚于开始时间");
        }
        String creationMode = normalized(request.creationMode());
        String trainingPolicy = normalized(request.trainingPolicy());
        if (!CREATION_MODES.contains(creationMode) || !TRAINING_POLICIES.contains(trainingPolicy)) {
            throw new IllegalArgumentException("创作模式或训练策略不合法");
        }
        List<String> activityStatuses = jdbcTemplate.query("""
                SELECT status FROM ops_content_activity
                WHERE id = ? AND tenant_id = ? AND deleted = 0 FOR UPDATE
                """, (rs, rowNum) -> rs.getString("status"), activityId, tenantId);
        if (activityStatuses.isEmpty()) throw new IllegalArgumentException("发布计划不存在");
        if ("TERMINATED".equals(activityStatuses.get(0))) {
            throw new IllegalArgumentException("已终止的计划不能编辑");
        }
        List<Long> planIds = jdbcTemplate.query("""
                SELECT id FROM ops_content_plan
                WHERE activity_id = ? AND tenant_id = ? AND deleted = 0
                ORDER BY id ASC LIMIT 1 FOR UPDATE
                """, (rs, rowNum) -> rs.getLong("id"), activityId, tenantId);
        if (planIds.isEmpty()) throw new IllegalArgumentException("计划内容不存在");
        Long planId = planIds.get(0);
        int storyboardCount = normalizeStoryboardCount(request.storyboardCount(), creationMode);
        jdbcTemplate.update("""
                UPDATE ops_content_activity
                SET activity_name = ?, objective = ?, start_time = ?, release_start_time = ?, end_time = ?,
                    version = version + 1, update_by = ?
                WHERE id = ? AND tenant_id = ? AND deleted = 0
                """, request.name().trim(), trimToNull(request.objective()), request.startTime(),
                request.releaseStartTime(), request.endTime(), operatorId, activityId, tenantId);
        jdbcTemplate.update("""
                UPDATE ops_content_plan
                SET plan_name = ?, task_instruction = ?, creation_mode = ?, storyboard_count = ?,
                    training_policy = ?, deadline = ?, version = version + 1, update_by = ?
                WHERE id = ? AND tenant_id = ? AND deleted = 0
                """, request.name().trim(), request.taskInstruction().trim(), creationMode, storyboardCount,
                trainingPolicy, request.endTime(), operatorId, planId, tenantId);

        if ("ACTIVE".equals(activityStatuses.get(0))) {
            List<Long> targets = normalizeEmployeeIds(request.employeeIds());
            if (targets.isEmpty()) throw new IllegalArgumentException("进行中的计划至少保留一名员工");
            MapSqlParameterSource params = new MapSqlParameterSource("tenantId", tenantId)
                    .addValue("planId", planId).addValue("employeeIds", targets)
                    .addValue("operatorId", operatorId);
            namedJdbcTemplate.update("""
                    UPDATE ops_content_employee_task
                    SET deleted = 1, version = version + 1, update_by = :operatorId
                    WHERE tenant_id = :tenantId AND plan_id = :planId AND deleted = 0
                      AND employee_id NOT IN (:employeeIds)
                      AND current_stage NOT IN ('COMPLETED', 'TERMINATED')
                    """, params);
            jdbcTemplate.update("""
                    UPDATE ops_content_employee_task
                    SET activity_name = ?, plan_name = ?, task_instruction = ?, storyboard_count = ?,
                        deadline = ?, version = version + 1, update_by = ?
                    WHERE tenant_id = ? AND plan_id = ? AND deleted = 0
                      AND current_stage NOT IN ('COMPLETED', 'TERMINATED')
                    """, request.name().trim(), request.name().trim(), request.taskInstruction().trim(),
                    storyboardCount, request.endTime(), operatorId, tenantId, planId);
            Long versionId = jdbcTemplate.query("""
                    SELECT id FROM ops_content_plan_version
                    WHERE tenant_id = ? AND plan_id = ? ORDER BY version_no DESC, id DESC LIMIT 1
                    """, rs -> rs.next() ? rs.getLong("id") : null, tenantId, planId);
            String initialStage = "NONE".equals(trainingPolicy) ? "READY_TO_SHOOT" : "LOCKED";
            for (Long employeeId : targets) {
                jdbcTemplate.update("""
                        INSERT INTO ops_content_employee_task
                            (tenant_id, plan_id, plan_version_id, employee_id, store_id,
                             activity_name, plan_name, task_instruction, storyboard_count,
                             current_stage, deadline, create_by, update_by)
                        SELECT ?, ?, ?, employee.id, employee.primary_store_id, ?, ?, ?, ?, ?, ?, ?, ?
                        FROM sys_merchant_user employee
                        WHERE employee.id = ? AND employee.tenant_id = ?
                          AND employee.status = 1 AND employee.login_enabled = 1 AND employee.deleted = 0
                          AND NOT EXISTS (
                            SELECT 1 FROM ops_content_employee_task existing
                            WHERE existing.tenant_id = ? AND existing.plan_id = ?
                              AND existing.employee_id = employee.id AND existing.deleted = 0
                        )
                        """, tenantId, planId, versionId, request.name().trim(), request.name().trim(),
                        request.taskInstruction().trim(), storyboardCount, initialStage, request.endTime(),
                        operatorId, operatorId, employeeId, tenantId, tenantId, planId);
            }
        }
    }

    public PlanPrecheckView precheck(Long tenantId, Long planId, List<Long> employeeIds) {
        requireDraftPlan(tenantId, planId);
        List<Long> targets = normalizeEmployeeIds(employeeIds);
        if (targets.isEmpty()) {
            throw new IllegalArgumentException("至少选择一名员工");
        }
        MapSqlParameterSource params = new MapSqlParameterSource("tenantId", tenantId)
                .addValue("employeeIds", targets);
        List<Long> active = namedJdbcTemplate.query("""
                SELECT id
                FROM sys_merchant_user
                WHERE tenant_id = :tenantId
                  AND id IN (:employeeIds)
                  AND status = 1 AND login_enabled = 1 AND deleted = 0
                """, params, (rs, rowNum) -> rs.getLong("id"));
        Set<Long> activeSet = Set.copyOf(active);
        List<PlanPrecheckView.TargetFailure> failures = targets.stream()
                .filter(id -> !activeSet.contains(id))
                .map(id -> new PlanPrecheckView.TargetFailure(
                        id, "EMPLOYEE_UNAVAILABLE", "员工不存在、已停用或未开通登录"))
                .toList();
        int duplicateCount = employeeIds == null ? 0 : employeeIds.size() - targets.size();
        return new PlanPrecheckView(
                employeeIds == null ? 0 : employeeIds.size(),
                active.size(),
                duplicateCount,
                failures.size(),
                failures
        );
    }

    public List<ActivitySummaryView> activities(
            Long tenantId, String keyword, String status, int page, int pageSize) {
        int safePage = Math.max(page, 1);
        int safePageSize = Math.min(Math.max(pageSize, 1), 100);
        StringBuilder where = new StringBuilder(
                " WHERE a.tenant_id = :tenantId AND a.deleted = 0 ");
        MapSqlParameterSource params = new MapSqlParameterSource("tenantId", tenantId);
        if (keyword != null && !keyword.isBlank()) {
            where.append(" AND a.activity_name LIKE :keyword ");
            params.addValue("keyword", "%" + keyword.trim() + "%");
        }
        if (status != null && !status.isBlank()) {
            where.append(" AND a.status = :status ");
            params.addValue("status", normalized(status));
        }
        params.addValue("pageSize", safePageSize)
                .addValue("offset", (safePage - 1) * safePageSize);
        List<ActivitySummaryView> activities = namedJdbcTemplate.query("""
                SELECT a.id, a.activity_name, a.objective, a.start_time, a.release_start_time, a.end_time,
                       a.status, a.owner_id, owner.display_name AS owner_name,
                       COUNT(DISTINCT p.id) AS plan_count,
                       COUNT(DISTINCT task.employee_id) AS employee_count,
                       COUNT(DISTINCT CASE WHEN task.current_stage = 'COMPLETED'
                                          THEN task.employee_id END) AS completed_count,
                       COUNT(DISTINCT CASE WHEN video.id IS NOT NULL
                                               AND (NULLIF(video.platform_video_id, '') IS NOT NULL
                                                    OR NULLIF(video.video_url, '') IS NOT NULL)
                                           THEN video.id END) AS completed_video_count,
                       MAX(p.creation_mode) AS creation_mode,
                       a.create_time
                FROM ops_content_activity a
                LEFT JOIN sys_merchant_user owner
                  ON owner.id = a.owner_id AND owner.tenant_id = a.tenant_id
                LEFT JOIN ops_content_plan p
                  ON p.activity_id = a.id AND p.tenant_id = a.tenant_id AND p.deleted = 0
                LEFT JOIN ops_content_employee_task task
                  ON task.plan_id = p.id AND task.tenant_id = a.tenant_id AND task.deleted = 0
                LEFT JOIN ops_content_video_performance video
                  ON video.task_id = task.id AND video.tenant_id = a.tenant_id AND video.deleted = 0
                """ + where + """
                GROUP BY a.id, a.activity_name, a.objective, a.start_time, a.release_start_time, a.end_time,
                         a.status, a.owner_id, owner.display_name, a.create_time
                ORDER BY a.create_time DESC, a.id DESC
                LIMIT :pageSize OFFSET :offset
                """, params, (rs, rowNum) -> new ActivitySummaryView(
                rs.getLong("id"), rs.getString("activity_name"), rs.getString("objective"),
                rs.getTimestamp("start_time").toLocalDateTime(),
                rs.getTimestamp("release_start_time").toLocalDateTime(),
                rs.getTimestamp("end_time").toLocalDateTime(),
                rs.getString("status"), rs.getLong("owner_id"), rs.getString("owner_name"),
                rs.getInt("plan_count"), rs.getInt("employee_count"),
                rs.getInt("completed_count"), rs.getInt("completed_video_count"),
                0, rs.getString("creation_mode"),
                rs.getTimestamp("create_time").toLocalDateTime()));
        return activities.stream().map(activity -> new ActivitySummaryView(
                activity.id(), activity.name(), activity.objective(), activity.startTime(),
                activity.releaseStartTime(), activity.endTime(), activity.status(), activity.ownerId(),
                activity.ownerName(), activity.planCount(), activity.employeeCount(), activity.completedCount(),
                activity.completedVideoCount(), requiredVideoCountFromTasks(tenantId, activity.id()),
                activity.creationMode(),
                activity.createdTime())).toList();
    }

    private int requiredVideoCountFromTasks(Long tenantId, Long activityId) {
        List<String> instructions = jdbcTemplate.queryForList("""
                SELECT task.task_instruction
                FROM ops_content_employee_task task
                JOIN ops_content_plan plan
                  ON plan.id = task.plan_id AND plan.tenant_id = task.tenant_id AND plan.deleted = 0
                WHERE task.tenant_id = ? AND plan.activity_id = ? AND task.deleted = 0
                """, String.class, tenantId, activityId);
        return instructions.stream().mapToInt(this::requiredVideoCount).sum();
    }

    private int requiredVideoCount(String taskInstruction) {
        if (taskInstruction == null || taskInstruction.isBlank()) return 0;
        Matcher matcher = Pattern.compile("=(\\d+)/(?:\\d+)").matcher(taskInstruction);
        int total = 0;
        while (matcher.find()) total += Integer.parseInt(matcher.group(1));
        return total;
    }

    public List<PlanSummaryView> plans(Long tenantId, Long activityId) {
        return jdbcTemplate.query("""
                SELECT p.id, p.activity_id, p.plan_name, p.task_instruction, p.creation_mode, p.storyboard_count,
                       p.training_policy, p.deadline, p.status, p.current_version_no,
                       COUNT(DISTINCT task.employee_id) AS employee_count
                FROM ops_content_plan p
                LEFT JOIN ops_content_employee_task task
                  ON task.plan_id = p.id AND task.tenant_id = p.tenant_id AND task.deleted = 0
                WHERE p.tenant_id = ? AND p.activity_id = ? AND p.deleted = 0
                GROUP BY p.id, p.activity_id, p.plan_name, p.task_instruction, p.creation_mode, p.storyboard_count,
                         p.training_policy, p.deadline, p.status, p.current_version_no
                ORDER BY p.create_time DESC, p.id DESC
                """, (rs, rowNum) -> new PlanSummaryView(
                rs.getLong("id"), rs.getLong("activity_id"), rs.getString("plan_name"),
                rs.getString("task_instruction"), rs.getString("creation_mode"),
                rs.getInt("storyboard_count"), rs.getString("training_policy"), rs.getTimestamp("deadline").toLocalDateTime(),
                rs.getString("status"), rs.getInt("current_version_no"),
                rs.getInt("employee_count")), tenantId, activityId);
    }

    @Transactional
    public void terminateActivity(Long tenantId, Long operatorId, Long activityId) {
        List<String> statuses = jdbcTemplate.query("""
                SELECT status
                FROM ops_content_activity
                WHERE id = ? AND tenant_id = ? AND deleted = 0
                FOR UPDATE
                """, (rs, rowNum) -> rs.getString("status"), activityId, tenantId);
        if (statuses.isEmpty()) {
            throw new IllegalArgumentException("发布计划不存在");
        }
        if ("TERMINATED".equals(statuses.get(0))) {
            return;
        }
        if (!List.of("ACTIVE", "PAUSED").contains(statuses.get(0))) {
            throw new IllegalArgumentException("只有进行中或已暂停的计划可以终止");
        }

        jdbcTemplate.update("""
                UPDATE ops_content_activity
                SET status = 'TERMINATED', version = version + 1, update_by = ?
                WHERE id = ? AND tenant_id = ? AND deleted = 0
                """, operatorId, activityId, tenantId);
        jdbcTemplate.update("""
                UPDATE ops_content_plan
                SET status = 'TERMINATED', version = version + 1, update_by = ?
                WHERE activity_id = ? AND tenant_id = ? AND deleted = 0
                  AND status <> 'TERMINATED'
                """, operatorId, activityId, tenantId);
        jdbcTemplate.update("""
                UPDATE ops_content_employee_task task
                JOIN ops_content_plan plan
                  ON plan.id = task.plan_id
                 AND plan.tenant_id = task.tenant_id
                SET task.current_stage = 'TERMINATED',
                    task.version = task.version + 1,
                    task.update_by = ?
                WHERE plan.activity_id = ?
                  AND task.tenant_id = ?
                  AND task.deleted = 0
                  AND task.current_stage NOT IN ('COMPLETED', 'TERMINATED')
                """, operatorId, activityId, tenantId);
    }

    @Transactional
    public void pauseActivity(Long tenantId, Long operatorId, Long activityId) {
        updateActivityExecutionStatus(tenantId, operatorId, activityId, "ACTIVE", "PAUSED", "只有进行中的计划可以暂停");
    }

    @Transactional
    public void resumeActivity(Long tenantId, Long operatorId, Long activityId) {
        updateActivityExecutionStatus(tenantId, operatorId, activityId, "PAUSED", "ACTIVE", "只有已暂停的计划可以恢复");
    }

    private void updateActivityExecutionStatus(
            Long tenantId,
            Long operatorId,
            Long activityId,
            String expectedStatus,
            String targetStatus,
            String invalidStatusMessage) {
        List<String> statuses = jdbcTemplate.query("""
                SELECT status FROM ops_content_activity
                WHERE id = ? AND tenant_id = ? AND deleted = 0
                FOR UPDATE
                """, (rs, rowNum) -> rs.getString("status"), activityId, tenantId);
        if (statuses.isEmpty()) throw new IllegalArgumentException("发布计划不存在");
        if (!expectedStatus.equals(statuses.get(0))) throw new IllegalArgumentException(invalidStatusMessage);

        jdbcTemplate.update("""
                UPDATE ops_content_activity
                SET status = ?, version = version + 1, update_by = ?
                WHERE id = ? AND tenant_id = ? AND deleted = 0
                """, targetStatus, operatorId, activityId, tenantId);
        jdbcTemplate.update("""
                UPDATE ops_content_plan
                SET status = ?, version = version + 1, update_by = ?
                WHERE activity_id = ? AND tenant_id = ? AND deleted = 0
                  AND status NOT IN ('TERMINATED', 'ENDED')
                """, targetStatus, operatorId, activityId, tenantId);
    }

    @Transactional
    public void deleteActivity(Long tenantId, Long operatorId, Long activityId) {
        List<ActivityDeleteSnapshot> activities = jdbcTemplate.query("""
                SELECT status, start_time
                FROM ops_content_activity
                WHERE id = ? AND tenant_id = ? AND deleted = 0
                FOR UPDATE
                """, (rs, rowNum) -> new ActivityDeleteSnapshot(
                rs.getString("status"), rs.getTimestamp("start_time").toLocalDateTime()),
                activityId, tenantId);
        if (activities.isEmpty()) {
            throw new IllegalArgumentException("发布计划不存在或已删除");
        }
        ActivityDeleteSnapshot activity = activities.get(0);
        boolean deletable = "DRAFT".equals(activity.status())
                || ("ACTIVE".equals(activity.status()) && activity.startTime().isAfter(LocalDateTime.now()));
        if (!deletable) {
            throw new IllegalArgumentException("只有草稿或待开始的计划可以删除");
        }
        jdbcTemplate.update("""
                UPDATE ops_content_employee_task task
                JOIN ops_content_plan plan
                  ON plan.id = task.plan_id AND plan.tenant_id = task.tenant_id
                SET task.deleted = 1, task.version = task.version + 1, task.update_by = ?
                WHERE plan.activity_id = ? AND task.tenant_id = ? AND task.deleted = 0
                """, operatorId, activityId, tenantId);
        jdbcTemplate.update("""
                UPDATE ops_content_plan
                SET deleted = 1, version = version + 1, update_by = ?
                WHERE activity_id = ? AND tenant_id = ? AND deleted = 0
                """, operatorId, activityId, tenantId);
        jdbcTemplate.update("""
                UPDATE ops_content_activity
                SET deleted = 1, version = version + 1, update_by = ?
                WHERE id = ? AND tenant_id = ? AND deleted = 0
                """, operatorId, activityId, tenantId);
    }

    @Transactional
    public PlanPublishView publish(
            Long tenantId,
            Long operatorId,
            Long planId,
            String idempotencyKey,
            List<Long> employeeIds) {
        if (idempotencyKey == null || idempotencyKey.isBlank() || idempotencyKey.length() > 128) {
            throw new IllegalArgumentException("发布计划必须提供有效的X-Idempotency-Key");
        }
        List<Long> targets = normalizeEmployeeIds(employeeIds);
        if (targets.isEmpty()) {
            throw new IllegalArgumentException("至少选择一名员工");
        }
        String fingerprint = sha256(planId + ":" + targets);
        PlanPublishView replay = findReplay(tenantId, planId, idempotencyKey.trim(), fingerprint);
        if (replay != null) {
            return replay;
        }

        long requestId;
        try {
            requestId = insertAndReturnId("""
                    INSERT INTO ops_content_publish_request
                        (tenant_id, plan_id, idempotency_key, request_fingerprint, status, create_by)
                    VALUES (?, ?, ?, ?, 'PROCESSING', ?)
                    """, tenantId, planId, idempotencyKey.trim(), fingerprint, operatorId);
        } catch (DuplicateKeyException duplicate) {
            PlanPublishView concurrentReplay =
                    findReplay(tenantId, planId, idempotencyKey.trim(), fingerprint);
            if (concurrentReplay != null) return concurrentReplay;
            throw new IllegalArgumentException("相同幂等键的发布请求正在处理中");
        }

        PlanSnapshot plan = requireDraftPlan(tenantId, planId);
        PlanPrecheckView precheck = precheck(tenantId, planId, targets);
        if (precheck.eligibleCount() == 0) {
            throw new IllegalArgumentException("没有可下发的有效员工");
        }
        int versionNo = plan.currentVersionNo() + 1;
        long planVersionId = insertAndReturnId("""
                INSERT INTO ops_content_plan_version
                    (tenant_id, plan_id, version_no, activity_name, plan_name, task_instruction,
                     creation_mode, storyboard_count, training_policy, deadline, published_by)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, tenantId, planId, versionNo, plan.activityName(), plan.planName(),
                plan.taskInstruction(), plan.creationMode(), plan.storyboardCount(), plan.trainingPolicy(),
                plan.deadline(), operatorId);

        List<Long> eligibleIds = targets.stream()
                .filter(id -> precheck.failures().stream().noneMatch(failure -> failure.employeeId().equals(id)))
                .toList();
        MapSqlParameterSource employeeParams = new MapSqlParameterSource("tenantId", tenantId)
                .addValue("employeeIds", eligibleIds);
        List<EmployeeTarget> employees = namedJdbcTemplate.query("""
                SELECT id, primary_store_id
                FROM sys_merchant_user
                WHERE tenant_id = :tenantId AND id IN (:employeeIds)
                  AND status = 1 AND login_enabled = 1 AND deleted = 0
                """, employeeParams, (rs, rowNum) -> new EmployeeTarget(
                rs.getLong("id"), rs.getObject("primary_store_id", Long.class)));
        String initialStage = "NONE".equals(plan.trainingPolicy()) ? "READY_TO_SHOOT" : "LOCKED";
        int created = 0;
        for (EmployeeTarget employee : employees) {
            created += jdbcTemplate.update("""
                    INSERT IGNORE INTO ops_content_employee_task
                        (tenant_id, plan_id, plan_version_id, employee_id, store_id,
                         activity_name, plan_name, task_instruction, storyboard_count, current_stage, deadline,
                         create_by, update_by)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """, tenantId, planId, planVersionId, employee.id(), employee.storeId(),
                    plan.activityName(), plan.planName(), plan.taskInstruction(), plan.storyboardCount(), initialStage,
                    plan.deadline(), operatorId, operatorId);
        }
        int changed = jdbcTemplate.update("""
                UPDATE ops_content_plan
                SET status = 'ACTIVE', current_version_no = ?, version = version + 1, update_by = ?
                WHERE id = ? AND tenant_id = ? AND status = 'DRAFT' AND version = ?
                """, versionNo, operatorId, planId, tenantId, plan.lockVersion());
        if (changed != 1) {
            throw new IllegalArgumentException("计划已被其他操作更新，请刷新后重试");
        }
        jdbcTemplate.update("""
                UPDATE ops_content_activity
                SET status = 'ACTIVE', version = version + 1, update_by = ?
                WHERE id = ? AND tenant_id = ? AND status = 'DRAFT'
                """, operatorId, plan.activityId(), tenantId);
        int failed = targets.size() - created;
        jdbcTemplate.update("""
                UPDATE ops_content_publish_request
                SET plan_version_id = ?, status = 'SUCCESS', created_count = ?, failed_count = ?
                WHERE id = ? AND tenant_id = ?
                """, planVersionId, created, failed, requestId, tenantId);
        return new PlanPublishView(
                planId, versionNo, failed == 0 ? "SUCCESS" : "PARTIAL_SUCCESS",
                created, failed, precheck.failures());
    }

    public List<EmployeeContentTaskView> employeeTasks(
            Long tenantId, Long employeeId, String category, int page, int pageSize) {
        int safePage = Math.max(page, 1);
        int safePageSize = Math.min(Math.max(pageSize, 1), 50);
        String normalizedCategory = normalizedCategory(category);
        return jdbcTemplate.query("""
                SELECT task.id, task.activity_name, task.plan_name, task.task_instruction,
                       plan.creation_mode, task.storyboard_count, activity.status AS plan_status,
                       task.current_stage,
                       task.deadline, task.completion_time, task.create_time
                FROM ops_content_employee_task task
                JOIN ops_content_plan plan
                  ON plan.id = task.plan_id AND plan.tenant_id = task.tenant_id AND plan.deleted = 0
                JOIN ops_content_activity activity
                  ON activity.id = plan.activity_id AND activity.tenant_id = task.tenant_id AND activity.deleted = 0
                WHERE task.tenant_id = ? AND task.employee_id = ? AND task.deleted = 0
                  AND (
                    ? = 'ALL'
                    OR (? = 'TODO' AND task.current_stage IN ('LOCKED','READY_TO_SHOOT','SHOOTING','NEEDS_REVISION','READY_TO_PUBLISH'))
                    OR (? = 'PROCESSING' AND task.current_stage IN ('PROCESSING','PENDING_REVIEW'))
                    OR (? = 'COMPLETED' AND task.current_stage = 'COMPLETED')
                    OR (? = 'EXCEPTION' AND task.current_stage IN ('EXPIRED','TERMINATED'))
                  )
                ORDER BY
                  CASE WHEN task.current_stage IN ('EXPIRED','TERMINATED','COMPLETED') THEN 1 ELSE 0 END,
                  task.deadline ASC, task.id DESC
                LIMIT ? OFFSET ?
                """, (rs, rowNum) -> toTaskView(
                rs.getLong("id"),
                rs.getString("activity_name"),
                rs.getString("plan_name"),
                rs.getString("task_instruction"),
                rs.getString("creation_mode"),
                rs.getInt("storyboard_count"),
                rs.getString("plan_status"),
                rs.getString("current_stage"),
                rs.getTimestamp("deadline").toLocalDateTime(),
                rs.getTimestamp("completion_time") == null
                        ? null : rs.getTimestamp("completion_time").toLocalDateTime(),
                rs.getTimestamp("create_time").toLocalDateTime()
        ), tenantId, employeeId, normalizedCategory, normalizedCategory, normalizedCategory,
                normalizedCategory, normalizedCategory, safePageSize, (safePage - 1) * safePageSize);
    }

    public EmployeeContentTaskView employeeTask(Long tenantId, Long employeeId, Long taskId) {
        List<EmployeeContentTaskView> records = jdbcTemplate.query("""
                SELECT task.id, task.activity_name, task.plan_name, task.task_instruction,
                       plan.creation_mode, task.storyboard_count, activity.status AS plan_status,
                       task.current_stage,
                       task.deadline, task.completion_time, task.create_time
                FROM ops_content_employee_task task
                JOIN ops_content_plan plan
                  ON plan.id = task.plan_id AND plan.tenant_id = task.tenant_id AND plan.deleted = 0
                JOIN ops_content_activity activity
                  ON activity.id = plan.activity_id AND activity.tenant_id = task.tenant_id AND activity.deleted = 0
                WHERE task.id = ? AND task.tenant_id = ? AND task.employee_id = ? AND task.deleted = 0
                """, (rs, rowNum) -> toTaskView(
                rs.getLong("id"),
                rs.getString("activity_name"),
                rs.getString("plan_name"),
                rs.getString("task_instruction"),
                rs.getString("creation_mode"),
                rs.getInt("storyboard_count"),
                rs.getString("plan_status"),
                rs.getString("current_stage"),
                rs.getTimestamp("deadline").toLocalDateTime(),
                rs.getTimestamp("completion_time") == null
                        ? null : rs.getTimestamp("completion_time").toLocalDateTime(),
                rs.getTimestamp("create_time").toLocalDateTime()
        ), taskId, tenantId, employeeId);
        if (records.isEmpty()) {
            throw new IllegalArgumentException("任务不存在");
        }
        return records.get(0);
    }

    public List<ContentDeliveryView> deliveryTasks(Long tenantId, Long activityId) {
        return jdbcTemplate.query("""
                SELECT task.id AS task_id, task.employee_id,
                       employee.employee_number, employee.display_name,
                       organization.org_name AS organization_name,
                       store_record.store_name,
                       task.current_stage, task.create_time, task.deadline, task.completion_time
                FROM ops_content_employee_task task
                JOIN ops_content_plan plan
                  ON plan.id = task.plan_id AND plan.tenant_id = task.tenant_id AND plan.deleted = 0
                LEFT JOIN sys_merchant_user employee
                  ON employee.id = task.employee_id AND employee.tenant_id = task.tenant_id
                LEFT JOIN sys_merchant_organization organization
                  ON organization.id = employee.organization_id AND organization.tenant_id = task.tenant_id
                LEFT JOIN sys_store store_record
                  ON store_record.id = task.store_id AND store_record.tenant_id = task.tenant_id
                WHERE task.tenant_id = ? AND plan.activity_id = ? AND task.deleted = 0
                ORDER BY task.id DESC
                """, (rs, rowNum) -> new ContentDeliveryView(
                rs.getLong("task_id"),
                rs.getLong("employee_id"),
                rs.getString("employee_number"),
                rs.getString("display_name"),
                rs.getString("organization_name"),
                rs.getString("store_name"),
                rs.getString("current_stage"),
                rs.getTimestamp("create_time").toLocalDateTime(),
                rs.getTimestamp("deadline").toLocalDateTime(),
                rs.getTimestamp("completion_time") == null
                        ? null : rs.getTimestamp("completion_time").toLocalDateTime()
        ), tenantId, activityId);
    }

    private PlanPublishView findReplay(
            Long tenantId, Long planId, String idempotencyKey, String fingerprint) {
        List<PublishRecord> records = jdbcTemplate.query("""
                SELECT r.plan_id, r.request_fingerprint, r.status, r.created_count, r.failed_count,
                       v.version_no
                FROM ops_content_publish_request r
                LEFT JOIN ops_content_plan_version v
                  ON v.id = r.plan_version_id AND v.tenant_id = r.tenant_id
                WHERE r.tenant_id = ? AND r.idempotency_key = ?
                """, (rs, rowNum) -> new PublishRecord(
                rs.getLong("plan_id"), rs.getString("request_fingerprint"),
                rs.getString("status"), rs.getInt("created_count"),
                rs.getInt("failed_count"), rs.getInt("version_no")
        ), tenantId, idempotencyKey);
        if (records.isEmpty()) return null;
        PublishRecord record = records.get(0);
        if (!record.planId().equals(planId) || !record.fingerprint().equals(fingerprint)) {
            throw new IllegalArgumentException("幂等键已用于其他发布请求");
        }
        if (!"SUCCESS".equals(record.status())) {
            throw new IllegalArgumentException("相同幂等键的发布请求正在处理中");
        }
        return new PlanPublishView(
                planId, record.versionNo(),
                record.failedCount() == 0 ? "SUCCESS" : "PARTIAL_SUCCESS",
                record.createdCount(), record.failedCount(), List.of());
    }

    private PlanSnapshot requireDraftPlan(Long tenantId, Long planId) {
        List<PlanSnapshot> records = jdbcTemplate.query("""
                SELECT p.activity_id, a.activity_name, p.plan_name, p.task_instruction,
                       p.creation_mode, p.storyboard_count, p.training_policy, p.deadline,
                       p.current_version_no, p.version
                FROM ops_content_plan p
                JOIN ops_content_activity a
                  ON a.id = p.activity_id AND a.tenant_id = p.tenant_id AND a.deleted = 0
                WHERE p.id = ? AND p.tenant_id = ? AND p.status = 'DRAFT' AND p.deleted = 0
                """, (rs, rowNum) -> new PlanSnapshot(
                rs.getLong("activity_id"), rs.getString("activity_name"),
                rs.getString("plan_name"), rs.getString("task_instruction"),
                rs.getString("creation_mode"), rs.getInt("storyboard_count"), rs.getString("training_policy"),
                rs.getTimestamp("deadline").toLocalDateTime(),
                rs.getInt("current_version_no"), rs.getInt("version")
        ), planId, tenantId);
        if (records.isEmpty()) {
            throw new IllegalArgumentException("计划不存在或当前状态不允许操作");
        }
        return records.get(0);
    }

    private void requireActiveEmployee(Long tenantId, Long employeeId) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM sys_merchant_user
                WHERE id = ? AND tenant_id = ?
                  AND status = 1 AND login_enabled = 1 AND deleted = 0
                """, Integer.class, employeeId, tenantId);
        if (count == null || count == 0) {
            throw new IllegalArgumentException("负责人不存在、已停用或未开通登录");
        }
    }

    private long insertAndReturnId(String sql, Object... args) {
        var keyHolder = new org.springframework.jdbc.support.GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement =
                    connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            for (int index = 0; index < args.length; index++) {
                statement.setObject(index + 1, args[index]);
            }
            return statement;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key == null) throw new IllegalStateException("未生成业务主键");
        return key.longValue();
    }

    private List<Long> normalizeEmployeeIds(List<Long> employeeIds) {
        if (employeeIds == null) return List.of();
        return new ArrayList<>(new LinkedHashSet<>(
                employeeIds.stream().filter(id -> id != null && id > 0).toList()));
    }

    private String normalizedCategory(String value) {
        String category = normalized(value);
        return Set.of("TODO", "PROCESSING", "COMPLETED", "EXCEPTION").contains(category)
                ? category : "ALL";
    }

    private void validateSchedule(LocalDateTime taskStartTime, LocalDateTime releaseStartTime) {
        LocalDateTime now = LocalDateTime.now();
        if (releaseStartTime == null || !releaseStartTime.isAfter(now.plusDays(3))) {
            throw new IllegalArgumentException("为确保流程正常进行，发布日期必须大于当前时间3天");
        }
        if (taskStartTime == null || taskStartTime.isBefore(now)
                || !taskStartTime.isBefore(releaseStartTime.toLocalDate().atStartOfDay())) {
            throw new IllegalArgumentException("任务开始时间只能选择当前时间到发布日前一天");
        }
    }

    private void validatePlatforms(List<String> platforms) {
        if (platforms == null || platforms.isEmpty()) {
            throw new IllegalArgumentException("发布平台至少选择一项");
        }
        if (platforms.stream().anyMatch(platform -> !PUBLISH_PLATFORMS.contains(platform))) {
            throw new IllegalArgumentException("发布平台不合法");
        }
    }

    private String normalized(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private int normalizeStoryboardCount(Integer value, String creationMode) {
        if ("SELF_CREATED".equals(creationMode)) return 1;
        if (value == null) return 3;
        return Math.min(Math.max(value, 1), 8);
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String sha256(String value) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("当前运行环境不支持SHA-256", exception);
        }
    }

    private EmployeeContentTaskView toTaskView(
            Long id,
            String activityName,
            String planName,
            String taskInstruction,
            String creationMode,
            int storyboardCount,
            String planStatus,
            String stage,
            LocalDateTime deadline,
            LocalDateTime completionTime,
            LocalDateTime createdTime) {
        TaskPresentation presentation = TaskPresentation.of(stage);
        return new EmployeeContentTaskView(
                id, activityName, planName, taskInstruction, creationMode, storyboardCount,
                planStatus, planStatusLabel(planStatus), stage,
                presentation.label(), presentation.hint(), presentation.category(),
                deadline, completionTime, createdTime);
    }

    private String planStatusLabel(String status) {
        return switch (status == null ? "" : status) {
            case "DRAFT" -> "未下发";
            case "ACTIVE" -> "进行中";
            case "PAUSED" -> "已暂停";
            case "TERMINATED" -> "已终止";
            default -> "状态更新中";
        };
    }

    private record PlanSnapshot(
            Long activityId,
            String activityName,
            String planName,
            String taskInstruction,
            String creationMode,
            int storyboardCount,
            String trainingPolicy,
            LocalDateTime deadline,
            int currentVersionNo,
            int lockVersion) {
    }

    private record EmployeeTarget(Long id, Long storeId) {
    }

    private record ActivityDeleteSnapshot(String status, LocalDateTime startTime) {
    }

    private record PublishRecord(
            Long planId,
            String fingerprint,
            String status,
            int createdCount,
            int failedCount,
            int versionNo) {
    }

    private record TaskPresentation(String label, String hint, String category) {
        private static TaskPresentation of(String stage) {
            return switch (stage) {
                case "LOCKED" -> new TaskPresentation(
                        "待解锁", "完成前置训练后即可开始拍摄", "TODO");
                case "READY_TO_SHOOT" -> new TaskPresentation(
                        "待拍摄", "查看任务要求并开始拍摄", "TODO");
                case "SHOOTING" -> new TaskPresentation(
                        "拍摄中", "继续完成剩余分镜", "TODO");
                case "PROCESSING" -> new TaskPresentation(
                        "处理中", "系统正在处理作品，无需重复提交", "PROCESSING");
                case "NEEDS_REVISION" -> new TaskPresentation(
                        "待整改", "按问题提示修改指定分镜", "TODO");
                case "PENDING_REVIEW" -> new TaskPresentation(
                        "待审核", "作品已提交，请等待审核结果", "PROCESSING");
                case "READY_TO_PUBLISH" -> new TaskPresentation(
                        "待发布", "下载成片并完成发布回传", "TODO");
                case "COMPLETED" -> new TaskPresentation(
                        "已完成", "任务已完成", "COMPLETED");
                case "EXPIRED" -> new TaskPresentation(
                        "已过期", "任务已超过截止时间，请联系负责人", "EXCEPTION");
                case "TERMINATED" -> new TaskPresentation(
                        "已终止", "任务已由负责人终止", "EXCEPTION");
                default -> new TaskPresentation(
                        "处理中", "任务状态更新中，请稍后查看", "PROCESSING");
            };
        }
    }
}
