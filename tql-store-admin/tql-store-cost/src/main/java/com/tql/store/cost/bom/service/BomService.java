package com.tql.store.cost.bom.service;

import com.tql.store.cost.bom.model.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class BomService {
    private final JdbcTemplate jdbcTemplate;
    private final BomCostCalculator costCalculator;

    public BomService(JdbcTemplate jdbcTemplate, BomCostCalculator costCalculator) {
        this.jdbcTemplate = jdbcTemplate;
        this.costCalculator = costCalculator;
    }

    public List<BomSummaryView> list(Long tenantId, Long storeId) {
        return jdbcTemplate.query("""
                SELECT id, store_id, dish_id, status, current_version, version, update_time
                FROM cost_bom
                WHERE tenant_id = ? AND store_id = ? AND deleted = 0
                ORDER BY update_time DESC
                """, (rs, rowNum) -> new BomSummaryView(
                rs.getLong("id"),
                rs.getLong("store_id"),
                rs.getLong("dish_id"),
                rs.getString("status"),
                rs.getInt("current_version"),
                rs.getInt("version"),
                rs.getTimestamp("update_time").toLocalDateTime()
        ), tenantId, storeId);
    }

    public BomDetailView detail(Long tenantId, Long bomId) {
        BomDetailView header = jdbcTemplate.query("""
                SELECT id, store_id, dish_id, status, current_version, version, remark, update_time
                FROM cost_bom
                WHERE tenant_id = ? AND id = ? AND deleted = 0
                """, rs -> {
            if (!rs.next()) {
                throw new IllegalArgumentException("BOM不存在");
            }
            return new BomDetailView(
                    rs.getLong("id"),
                    rs.getLong("store_id"),
                    rs.getLong("dish_id"),
                    rs.getString("status"),
                    rs.getInt("current_version"),
                    rs.getInt("version"),
                    rs.getString("remark"),
                    rs.getTimestamp("update_time").toLocalDateTime(),
                    Collections.emptyList()
            );
        }, tenantId, bomId);
        List<BomDetailView.Item> items = jdbcTemplate.query("""
                SELECT id, material_id, unit_id, quantity, sort_order
                FROM cost_bom_item
                WHERE tenant_id = ? AND bom_id = ? AND version_no = ?
                ORDER BY sort_order, id
                """, (rs, rowNum) -> new BomDetailView.Item(
                rs.getLong("id"),
                rs.getLong("material_id"),
                rs.getLong("unit_id"),
                rs.getBigDecimal("quantity"),
                rs.getInt("sort_order")
        ), tenantId, bomId, header.bomVersion());
        return new BomDetailView(
                header.id(), header.storeId(), header.dishId(), header.status(),
                header.bomVersion(), header.rowVersion(), header.remark(), header.updatedTime(), items);
    }

    @Transactional
    public Long createDraft(Long tenantId, Long userId, CreateBomRequest request) {
        assertStoreBelongsToTenant(tenantId, request.storeId());
        assertDishBelongsToTenant(tenantId, request.dishId());
        validateItems(tenantId, request.items().stream()
                .map(item -> new ItemIdentity(item.materialId(), item.unitId())).toList());
        var key = new org.springframework.jdbc.support.GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO cost_bom
                      (tenant_id, store_id, dish_id, status, current_version, remark, created_by, updated_by)
                    VALUES (?, ?, ?, 'DRAFT', 1, ?, ?, ?)
                    """, Statement.RETURN_GENERATED_KEYS);
            statement.setLong(1, tenantId);
            statement.setLong(2, request.storeId());
            statement.setLong(3, request.dishId());
            statement.setString(4, request.remark());
            statement.setLong(5, userId);
            statement.setLong(6, userId);
            return statement;
        }, key);
        Long bomId = key.getKey().longValue();
        jdbcTemplate.update("""
                INSERT INTO cost_bom_version
                  (tenant_id, bom_id, version_no, status, remark, created_by)
                VALUES (?, ?, 1, 'DRAFT', ?, ?)
                """, tenantId, bomId, request.remark(), userId);
        List<CreateBomRequest.Item> items = request.items();
        jdbcTemplate.batchUpdate("""
                INSERT INTO cost_bom_item
                  (tenant_id, bom_id, version_no, material_id, unit_id, quantity, sort_order, created_by)
                VALUES (?, ?, 1, ?, ?, ?, ?, ?)
                """, items, items.size(), (statement, item) -> {
            statement.setLong(1, tenantId);
            statement.setLong(2, bomId);
            statement.setLong(3, item.materialId());
            statement.setLong(4, item.unitId());
            statement.setBigDecimal(5, item.quantity());
            statement.setInt(6, item.sortOrder() == null ? 0 : item.sortOrder());
            statement.setLong(7, userId);
        });
        return bomId;
    }

    @Transactional
    public void updateDraft(Long tenantId, Long userId, Long bomId, UpdateBomRequest request) {
        BomDetailView current = detail(tenantId, bomId);
        BomStatus status = BomStatus.valueOf(current.status());
        if (status != BomStatus.DRAFT && status != BomStatus.REJECTED) {
            throw new IllegalArgumentException("仅草稿或已驳回BOM允许编辑");
        }
        validateItems(tenantId, request.items().stream()
                .map(item -> new ItemIdentity(item.materialId(), item.unitId())).toList());
        int updated = jdbcTemplate.update("""
                UPDATE cost_bom
                SET status = 'DRAFT', remark = ?, updated_by = ?, version = version + 1
                WHERE tenant_id = ? AND id = ? AND version = ? AND deleted = 0
                  AND status IN ('DRAFT', 'REJECTED')
                """, request.remark(), userId, tenantId, bomId, request.expectedVersion());
        requireSingleUpdate(updated);
        jdbcTemplate.update("""
                UPDATE cost_bom_version
                SET status = 'DRAFT', remark = ?
                WHERE tenant_id = ? AND bom_id = ? AND version_no = ?
                """, request.remark(), tenantId, bomId, current.bomVersion());
        jdbcTemplate.update("""
                DELETE FROM cost_bom_item
                WHERE tenant_id = ? AND bom_id = ? AND version_no = ?
                """, tenantId, bomId, current.bomVersion());
        insertItems(tenantId, userId, bomId, current.bomVersion(), request.items().stream()
                .map(item -> new WritableItem(item.materialId(), item.unitId(), item.quantity(), item.sortOrder()))
                .toList());
    }

    @Transactional
    public void submit(Long tenantId, Long userId, Long bomId, BomTransitionRequest request) {
        transition(tenantId, userId, bomId, request.expectedVersion(), BomStatus.PENDING, request.remark());
    }

    @Transactional
    public void reject(Long tenantId, Long userId, Long bomId, BomTransitionRequest request) {
        if (request.remark() == null || request.remark().isBlank()) {
            throw new IllegalArgumentException("驳回原因不能为空");
        }
        transition(tenantId, userId, bomId, request.expectedVersion(), BomStatus.REJECTED, request.remark());
    }

    @Transactional
    public void publish(Long tenantId, Long userId, Long bomId, BomTransitionRequest request) {
        BomDetailView current = detail(tenantId, bomId);
        BomStatus source = BomStatus.valueOf(current.status());
        source.requireTransitionTo(BomStatus.PUBLISHED);
        java.math.BigDecimal totalCost =
                calculateAndSnapshotCost(tenantId, current.storeId(), bomId, current.bomVersion());
        int updated = jdbcTemplate.update("""
                UPDATE cost_bom
                SET status = 'PUBLISHED', remark = COALESCE(?, remark),
                    updated_by = ?, version = version + 1
                WHERE tenant_id = ? AND id = ? AND version = ? AND status = ? AND deleted = 0
                """, request.remark(), userId, tenantId, bomId, request.expectedVersion(), source.name());
        requireSingleUpdate(updated);
        jdbcTemplate.update("""
                UPDATE cost_bom_version
                SET status = 'PUBLISHED', total_cost = ?, effective_from = ?,
                    published_by = ?, published_time = ?
                WHERE tenant_id = ? AND bom_id = ? AND version_no = ? AND status = 'PENDING'
                """, totalCost, LocalDateTime.now(), userId, LocalDateTime.now(),
                tenantId, bomId, current.bomVersion());
    }

    private java.math.BigDecimal calculateAndSnapshotCost(
            Long tenantId, Long storeId, Long bomId, Integer bomVersion) {
        List<CostSourceItem> items = jdbcTemplate.query("""
                SELECT item.id, item.material_id, item.unit_id, item.quantity, material.base_unit_id
                FROM cost_bom_item item
                JOIN cost_material material
                  ON material.id = item.material_id
                 AND material.tenant_id = item.tenant_id
                 AND material.status = 1
                 AND material.deleted = 0
                WHERE item.tenant_id = ? AND item.bom_id = ? AND item.version_no = ?
                ORDER BY item.id
                """, (rs, rowNum) -> new CostSourceItem(
                rs.getLong("id"),
                rs.getLong("material_id"),
                rs.getLong("unit_id"),
                rs.getLong("base_unit_id"),
                rs.getBigDecimal("quantity")
        ), tenantId, bomId, bomVersion);
        if (items.isEmpty()) {
            throw new IllegalArgumentException("BOM没有物料明细，不能发布");
        }
        java.math.BigDecimal total = java.math.BigDecimal.ZERO;
        LocalDateTime costingTime = LocalDateTime.now();
        for (CostSourceItem item : items) {
            java.math.BigDecimal conversionRate = item.unitId().equals(item.baseUnitId())
                    ? java.math.BigDecimal.ONE
                    : findConversionRate(tenantId, item.materialId(), item.unitId(), item.baseUnitId());
            java.math.BigDecimal unitPrice =
                    findEffectivePrice(tenantId, storeId, item.materialId(), costingTime);
            BomCostCalculator.CostResult result =
                    costCalculator.calculate(item.quantity(), conversionRate, unitPrice);
            int updated = jdbcTemplate.update("""
                    UPDATE cost_bom_item
                    SET converted_quantity = ?, unit_price = ?, item_cost = ?
                    WHERE tenant_id = ? AND id = ? AND bom_id = ? AND version_no = ?
                    """, result.convertedQuantity(), result.unitPrice(), result.itemCost(),
                    tenantId, item.id(), bomId, bomVersion);
            if (updated != 1) {
                throw new IllegalArgumentException("BOM成本快照生成失败");
            }
            total = total.add(result.itemCost());
        }
        return total.setScale(4, java.math.RoundingMode.HALF_UP);
    }

    private java.math.BigDecimal findConversionRate(
            Long tenantId, Long materialId, Long sourceUnitId, Long targetUnitId) {
        List<java.math.BigDecimal> rates = jdbcTemplate.query("""
                SELECT conversion_rate
                FROM cost_material_unit_conversion
                WHERE tenant_id = ? AND material_id = ?
                  AND source_unit_id = ? AND target_unit_id = ? AND deleted = 0
                LIMIT 1
                """, (rs, rowNum) -> rs.getBigDecimal("conversion_rate"),
                tenantId, materialId, sourceUnitId, targetUnitId);
        if (rates.isEmpty()) {
            throw new IllegalArgumentException("物料" + materialId + "缺少到基本单位的换算关系");
        }
        return rates.get(0);
    }

    private java.math.BigDecimal findEffectivePrice(
            Long tenantId, Long storeId, Long materialId, LocalDateTime costingTime) {
        List<java.math.BigDecimal> prices = jdbcTemplate.query("""
                SELECT unit_price
                FROM cost_material_price
                WHERE tenant_id = ? AND store_id = ? AND material_id = ? AND deleted = 0
                  AND effective_from <= ?
                  AND (effective_to IS NULL OR effective_to > ?)
                ORDER BY FIELD(price_type, 'MANUAL', 'STANDARD', 'PURCHASE'), effective_from DESC
                LIMIT 1
                """, (rs, rowNum) -> rs.getBigDecimal("unit_price"),
                tenantId, storeId, materialId, costingTime, costingTime);
        if (prices.isEmpty()) {
            throw new IllegalArgumentException("物料" + materialId + "缺少当前门店的生效价格");
        }
        return prices.get(0);
    }

    private void transition(
            Long tenantId,
            Long userId,
            Long bomId,
            Integer expectedVersion,
            BomStatus target,
            String remark) {
        BomDetailView current = detail(tenantId, bomId);
        BomStatus source = BomStatus.valueOf(current.status());
        source.requireTransitionTo(target);
        int updated = jdbcTemplate.update("""
                UPDATE cost_bom
                SET status = ?, remark = COALESCE(?, remark),
                    updated_by = ?, version = version + 1
                WHERE tenant_id = ? AND id = ? AND version = ? AND status = ? AND deleted = 0
                """, target.name(), remark, userId, tenantId, bomId, expectedVersion, source.name());
        requireSingleUpdate(updated);
        jdbcTemplate.update("""
                UPDATE cost_bom_version
                SET status = ?, remark = COALESCE(?, remark)
                WHERE tenant_id = ? AND bom_id = ? AND version_no = ? AND status = ?
                """, target.name(), remark, tenantId, bomId, current.bomVersion(), source.name());
    }

    private void requireSingleUpdate(int updated) {
        if (updated != 1) {
            throw new IllegalArgumentException("BOM已被其他用户修改，请刷新后重试");
        }
    }

    private void validateItems(Long tenantId, List<ItemIdentity> items) {
        if (items.stream().map(ItemIdentity::materialId).distinct().count() != items.size()) {
            throw new IllegalArgumentException("同一BOM版本不能重复添加物料");
        }
        List<Long> materialIds = items.stream().map(ItemIdentity::materialId).distinct().toList();
        List<Long> unitIds = items.stream().map(ItemIdentity::unitId).distinct().toList();
        assertOwnedActiveIds("cost_material", tenantId, materialIds, "物料");
        assertOwnedActiveIds("cost_material_unit", tenantId, unitIds, "单位");
    }

    private void assertOwnedActiveIds(String table, Long tenantId, List<Long> ids, String label) {
        if (ids.isEmpty()) {
            throw new IllegalArgumentException(label + "不能为空");
        }
        String placeholders = String.join(",", Collections.nCopies(ids.size(), "?"));
        List<Object> parameters = new ArrayList<>();
        parameters.add(tenantId);
        parameters.addAll(ids);
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + table
                        + " WHERE tenant_id = ? AND status = 1 AND deleted = 0 AND id IN (" + placeholders + ")",
                Integer.class, parameters.toArray());
        if (count == null || count != ids.size()) {
            throw new IllegalArgumentException(label + "不存在、已停用或不属于当前商户");
        }
    }

    private void assertDishBelongsToTenant(Long tenantId, Long dishId) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM cost_dish
                WHERE id = ? AND tenant_id = ? AND status = 1 AND deleted = 0
                """, Integer.class, dishId, tenantId);
        if (count == null || count == 0) {
            throw new IllegalArgumentException("菜品不存在、已停用或不属于当前商户");
        }
    }

    private void insertItems(
            Long tenantId, Long userId, Long bomId, Integer bomVersion, List<WritableItem> items) {
        jdbcTemplate.batchUpdate("""
                INSERT INTO cost_bom_item
                  (tenant_id, bom_id, version_no, material_id, unit_id, quantity, sort_order, created_by)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """, items, items.size(), (statement, item) -> {
            statement.setLong(1, tenantId);
            statement.setLong(2, bomId);
            statement.setInt(3, bomVersion);
            statement.setLong(4, item.materialId());
            statement.setLong(5, item.unitId());
            statement.setBigDecimal(6, item.quantity());
            statement.setInt(7, item.sortOrder() == null ? 0 : item.sortOrder());
            statement.setLong(8, userId);
        });
    }

    private void assertStoreBelongsToTenant(Long tenantId, Long storeId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_store WHERE id = ? AND tenant_id = ? AND status = 1",
                Integer.class, storeId, tenantId);
        if (count == null || count == 0) {
            throw new IllegalArgumentException("门店不存在或不属于当前商户");
        }
    }

    private record ItemIdentity(Long materialId, Long unitId) {
    }

    private record WritableItem(
            Long materialId,
            Long unitId,
            java.math.BigDecimal quantity,
            Integer sortOrder) {
    }

    private record CostSourceItem(
            Long id,
            Long materialId,
            Long unitId,
            Long baseUnitId,
            java.math.BigDecimal quantity) {
    }
}
