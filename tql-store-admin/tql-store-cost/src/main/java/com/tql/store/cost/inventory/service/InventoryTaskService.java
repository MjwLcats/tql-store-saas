package com.tql.store.cost.inventory.service;

import com.tql.store.cost.inventory.model.CreateInventoryTaskRequest;
import com.tql.store.cost.inventory.model.InventoryTaskView;
import com.tql.store.cost.inventory.model.InventoryCountModels.CountItemView;
import com.tql.store.cost.inventory.model.InventoryCountModels.SubmitCountsRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.math.BigDecimal;

@Service
public class InventoryTaskService {
    private final JdbcTemplate jdbcTemplate;

    public InventoryTaskService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<InventoryTaskView> list(Long tenantId, Long storeId) {
        return jdbcTemplate.query("""
                SELECT id, store_id, task_code, task_name, status,
                       planned_start_time, planned_end_time, version
                FROM cost_inventory_task
                WHERE tenant_id = ? AND store_id = ? AND deleted = 0
                ORDER BY id DESC
                """, (rs, rowNum) -> new InventoryTaskView(
                rs.getLong("id"),
                rs.getLong("store_id"),
                rs.getString("task_code"),
                rs.getString("task_name"),
                rs.getString("status"),
                rs.getTimestamp("planned_start_time").toLocalDateTime(),
                rs.getTimestamp("planned_end_time").toLocalDateTime(),
                rs.getInt("version")
        ), tenantId, storeId);
    }

    @Transactional
    public Long create(Long tenantId, Long userId, CreateInventoryTaskRequest request) {
        if (!request.plannedEndTime().isAfter(request.plannedStartTime())) {
            throw new IllegalArgumentException("计划结束时间必须晚于开始时间");
        }
        assertStoreBelongsToTenant(tenantId, request.storeId());
        String taskCode = request.plannedStartTime().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        var key = new org.springframework.jdbc.support.GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO cost_inventory_task
                      (tenant_id, store_id, task_code, task_name, status,
                       planned_start_time, planned_end_time, remark, created_by, updated_by)
                    VALUES (?, ?, ?, ?, 'DRAFT', ?, ?, ?, ?, ?)
                    """, Statement.RETURN_GENERATED_KEYS);
            statement.setLong(1, tenantId);
            statement.setLong(2, request.storeId());
            statement.setString(3, taskCode);
            statement.setString(4, request.taskName());
            statement.setObject(5, request.plannedStartTime());
            statement.setObject(6, request.plannedEndTime());
            statement.setString(7, request.remark());
            statement.setLong(8, userId);
            statement.setLong(9, userId);
            return statement;
        }, key);
        return key.getKey().longValue();
    }

    public Long storeId(Long tenantId, Long taskId) {
        List<Long> values = jdbcTemplate.query(
                "SELECT store_id FROM cost_inventory_task WHERE id = ? AND tenant_id = ? AND deleted = 0",
                (rs, rowNum) -> rs.getLong(1), taskId, tenantId);
        if (values.isEmpty()) throw new IllegalArgumentException("盘点任务不存在");
        return values.get(0);
    }

    @Transactional
    public List<CountItemView> start(Long tenantId, Long userId, Long taskId) {
        Long storeId = storeId(tenantId, taskId);
        String status = jdbcTemplate.queryForObject(
                "SELECT status FROM cost_inventory_task WHERE id = ? AND tenant_id = ? FOR UPDATE",
                String.class, taskId, tenantId);
        if ("DRAFT".equals(status)) {
            jdbcTemplate.update("""
                    INSERT INTO cost_inventory_snapshot
                      (tenant_id, store_id, task_id, location_code, location_name, material_id,
                       material_code, material_name, specification, count_unit_id, count_unit_name,
                       conversion_rate, book_quantity, unit_price)
                    SELECT m.tenant_id, ?, ?, 'DEFAULT', '默认库位', m.id, m.material_code,
                           m.material_name, m.specification, m.base_unit_id, u.unit_name, 1, 0,
                           (SELECT p.unit_price FROM cost_material_price p
                            WHERE p.tenant_id=m.tenant_id AND p.store_id=? AND p.material_id=m.id
                              AND p.deleted=0 AND p.effective_from<=NOW()
                              AND (p.effective_to IS NULL OR p.effective_to>NOW())
                            ORDER BY p.effective_from DESC LIMIT 1)
                    FROM cost_material m
                    JOIN cost_material_unit u ON u.id=m.base_unit_id AND u.tenant_id=m.tenant_id
                    WHERE m.tenant_id=? AND m.status=1 AND m.deleted=0
                    """, storeId, taskId, storeId, tenantId);
            jdbcTemplate.update("""
                    UPDATE cost_inventory_task
                    SET status='IN_PROGRESS', updated_by=?, version=version+1
                    WHERE id=? AND tenant_id=? AND status='DRAFT'
                    """, userId, taskId, tenantId);
        } else if (!"IN_PROGRESS".equals(status) && !"REJECTED".equals(status)) {
            throw new IllegalStateException("当前任务状态不能继续盘点");
        }
        return items(tenantId, userId, taskId);
    }

    public List<CountItemView> items(Long tenantId, Long userId, Long taskId) {
        return jdbcTemplate.query("""
                SELECT s.id, s.material_code, s.material_name, s.specification, s.location_name,
                       s.count_unit_name, s.book_quantity,
                       (SELECT c.counted_quantity FROM cost_inventory_count c
                        WHERE c.tenant_id=s.tenant_id AND c.task_id=s.task_id
                          AND c.snapshot_id=s.id AND c.counter_id=?
                        ORDER BY c.count_round DESC LIMIT 1) counted_quantity
                FROM cost_inventory_snapshot s
                WHERE s.tenant_id=? AND s.task_id=?
                ORDER BY s.location_name, s.material_code
                """, (rs, rowNum) -> new CountItemView(
                rs.getLong("id"), rs.getString("material_code"), rs.getString("material_name"),
                rs.getString("specification"), rs.getString("location_name"),
                rs.getString("count_unit_name"), rs.getBigDecimal("book_quantity"),
                rs.getBigDecimal("counted_quantity")
        ), userId, tenantId, taskId);
    }

    @Transactional
    public void submit(Long tenantId, Long storeId, Long userId, Long taskId, SubmitCountsRequest request) {
        String status = jdbcTemplate.queryForObject(
                "SELECT status FROM cost_inventory_task WHERE id=? AND tenant_id=? FOR UPDATE",
                String.class, taskId, tenantId);
        if ("PENDING_REVIEW".equals(status)) return;
        if (!"IN_PROGRESS".equals(status) && !"REJECTED".equals(status)) {
            throw new IllegalStateException("当前任务状态不能提交");
        }
        Integer snapshotCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM cost_inventory_snapshot WHERE tenant_id=? AND task_id=?",
                Integer.class, tenantId, taskId);
        if (snapshotCount == null || snapshotCount != request.items().size()) {
            throw new IllegalArgumentException("请完成全部物料盘点后再提交");
        }
        for (var item : request.items()) {
            BigDecimal rate = jdbcTemplate.queryForObject("""
                    SELECT conversion_rate FROM cost_inventory_snapshot
                    WHERE id=? AND tenant_id=? AND task_id=?
                    """, BigDecimal.class, item.snapshotId(), tenantId, taskId);
            if (rate == null) throw new IllegalArgumentException("盘点物料不存在");
            jdbcTemplate.update("""
                    INSERT INTO cost_inventory_count
                      (tenant_id, store_id, task_id, snapshot_id, counter_id, count_round,
                       counted_quantity, base_quantity, status, idempotency_key, submitted_time)
                    VALUES (?, ?, ?, ?, ?, 1, ?, ?, 'SUBMITTED', ?, NOW())
                    ON DUPLICATE KEY UPDATE counted_quantity=VALUES(counted_quantity),
                      base_quantity=VALUES(base_quantity), status='SUBMITTED', submitted_time=NOW(),
                      version=version+1
                    """, tenantId, storeId, taskId, item.snapshotId(), userId,
                    item.countedQuantity(), item.countedQuantity().multiply(rate),
                    request.idempotencyKey() + ":" + item.snapshotId());
        }
        jdbcTemplate.update("""
                UPDATE cost_inventory_task
                SET status='PENDING_REVIEW', submitted_time=NOW(), updated_by=?, version=version+1
                WHERE id=? AND tenant_id=?
                """, userId, taskId, tenantId);
    }

    @Transactional
    public void review(Long tenantId, Long userId, Long taskId, Integer expectedVersion,
                       boolean approved, String remark) {
        String next = approved ? "APPROVED" : "REJECTED";
        int updated = jdbcTemplate.update("""
                UPDATE cost_inventory_task
                SET status=?, approved_time=?, approved_by=?, remark=COALESCE(?,remark),
                    updated_by=?, version=version+1
                WHERE id=? AND tenant_id=? AND status='PENDING_REVIEW' AND version=?
                """, next, approved ? java.time.LocalDateTime.now() : null,
                approved ? userId : null, remark, userId, taskId, tenantId, expectedVersion);
        if (updated == 0) throw new IllegalStateException("任务已被处理，请刷新后重试");
        jdbcTemplate.update("""
                UPDATE cost_inventory_count SET status=?, version=version+1
                WHERE tenant_id=? AND task_id=? AND status='SUBMITTED'
                """, approved ? "APPROVED" : "REJECTED", tenantId, taskId);
    }

    @Transactional
    public void close(Long tenantId, Long userId, Long taskId, Integer expectedVersion) {
        int updated = jdbcTemplate.update("""
                UPDATE cost_inventory_task
                SET status='CLOSED', closed_time=NOW(), updated_by=?, version=version+1
                WHERE id=? AND tenant_id=? AND status='APPROVED' AND version=?
                """, userId, taskId, tenantId, expectedVersion);
        if (updated == 0) throw new IllegalStateException("只有已审核任务可以关账，请刷新后重试");
    }

    private void assertStoreBelongsToTenant(Long tenantId, Long storeId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_store WHERE id = ? AND tenant_id = ? AND status = 1",
                Integer.class, storeId, tenantId);
        if (count == null || count == 0) {
            throw new IllegalArgumentException("门店不存在或不属于当前商户");
        }
    }
}
