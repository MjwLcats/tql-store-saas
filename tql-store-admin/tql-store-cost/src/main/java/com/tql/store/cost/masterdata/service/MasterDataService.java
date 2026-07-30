package com.tql.store.cost.masterdata.service;

import com.tql.store.cost.masterdata.model.MasterDataModels.*;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Locale;

@Service
public class MasterDataService {
    private final JdbcTemplate jdbcTemplate;

    public MasterDataService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<UnitView> units(Long tenantId) {
        return jdbcTemplate.query("""
                SELECT id, unit_code, unit_name, decimal_scale, status
                FROM cost_material_unit
                WHERE tenant_id = ? AND deleted = 0
                ORDER BY unit_code
                """, (rs, rowNum) -> new UnitView(
                rs.getLong("id"), rs.getString("unit_code"), rs.getString("unit_name"),
                rs.getInt("decimal_scale"), rs.getInt("status")), tenantId);
    }

    public Long createUnit(Long tenantId, Long userId, CreateUnitRequest request) {
        return insertWithDuplicateMessage("""
                INSERT INTO cost_material_unit
                  (tenant_id, unit_code, unit_name, decimal_scale, created_by, updated_by)
                VALUES (?, ?, ?, ?, ?, ?)
                """, "单位编码已存在", tenantId, normalizeCode(request.unitCode()),
                request.unitName().trim(), request.decimalScale(), userId, userId);
    }

    public List<MaterialView> materials(Long tenantId) {
        return jdbcTemplate.query("""
                SELECT id, material_code, material_name, specification, base_unit_id,
                       external_material_code, source_system, status
                FROM cost_material
                WHERE tenant_id = ? AND deleted = 0
                ORDER BY material_code
                """, (rs, rowNum) -> new MaterialView(
                rs.getLong("id"), rs.getString("material_code"), rs.getString("material_name"),
                rs.getString("specification"), rs.getLong("base_unit_id"),
                rs.getString("external_material_code"), rs.getString("source_system"),
                rs.getInt("status")), tenantId);
    }

    @Transactional
    public Long createMaterial(Long tenantId, Long userId, CreateMaterialRequest request) {
        requireUnit(tenantId, request.baseUnitId());
        return insertWithDuplicateMessage("""
                INSERT INTO cost_material
                  (tenant_id, material_code, material_name, specification, base_unit_id,
                   external_material_code, source_system, created_by, updated_by)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, "物料编码或外部物料编码已存在", tenantId, normalizeCode(request.materialCode()),
                request.materialName().trim(), trimToNull(request.specification()), request.baseUnitId(),
                trimToNull(request.externalMaterialCode()), normalizeSource(request.sourceSystem()), userId, userId);
    }

    public List<DishView> dishes(Long tenantId) {
        return jdbcTemplate.query("""
                SELECT id, dish_code, dish_name, external_dish_code, source_system, status
                FROM cost_dish
                WHERE tenant_id = ? AND deleted = 0
                ORDER BY dish_code
                """, (rs, rowNum) -> new DishView(
                rs.getLong("id"), rs.getString("dish_code"), rs.getString("dish_name"),
                rs.getString("external_dish_code"), rs.getString("source_system"),
                rs.getInt("status")), tenantId);
    }

    public Long createDish(Long tenantId, Long userId, CreateDishRequest request) {
        return insertWithDuplicateMessage("""
                INSERT INTO cost_dish
                  (tenant_id, dish_code, dish_name, external_dish_code, source_system, created_by, updated_by)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, "菜品编码或外部菜品编码已存在", tenantId, normalizeCode(request.dishCode()),
                request.dishName().trim(), trimToNull(request.externalDishCode()),
                normalizeSource(request.sourceSystem()), userId, userId);
    }

    public List<ConversionView> conversions(Long tenantId, Long materialId) {
        requireMaterial(tenantId, materialId);
        return jdbcTemplate.query("""
                SELECT id, material_id, source_unit_id, target_unit_id, conversion_rate
                FROM cost_material_unit_conversion
                WHERE tenant_id = ? AND material_id = ? AND deleted = 0
                ORDER BY id
                """, (rs, rowNum) -> new ConversionView(
                rs.getLong("id"), rs.getLong("material_id"),
                rs.getLong("source_unit_id"), rs.getLong("target_unit_id"),
                rs.getBigDecimal("conversion_rate")), tenantId, materialId);
    }

    @Transactional
    public Long createConversion(Long tenantId, Long userId, CreateConversionRequest request) {
        if (request.sourceUnitId().equals(request.targetUnitId())) {
            throw new IllegalArgumentException("来源单位和目标单位不能相同");
        }
        requireMaterial(tenantId, request.materialId());
        requireUnit(tenantId, request.sourceUnitId());
        requireUnit(tenantId, request.targetUnitId());
        Long baseUnitId = jdbcTemplate.queryForObject("""
                SELECT base_unit_id FROM cost_material
                WHERE tenant_id = ? AND id = ? AND status = 1 AND deleted = 0
                """, Long.class, tenantId, request.materialId());
        if (!request.targetUnitId().equals(baseUnitId)) {
            throw new IllegalArgumentException("目标单位必须是物料基本单位");
        }
        return insertWithDuplicateMessage("""
                INSERT INTO cost_material_unit_conversion
                  (tenant_id, material_id, source_unit_id, target_unit_id, conversion_rate,
                   created_by, updated_by)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, "该物料单位换算关系已存在", tenantId, request.materialId(),
                request.sourceUnitId(), request.targetUnitId(), request.conversionRate(), userId, userId);
    }

    public List<PriceView> prices(Long tenantId, Long storeId, Long materialId) {
        requireStore(tenantId, storeId);
        requireMaterial(tenantId, materialId);
        return jdbcTemplate.query("""
                SELECT id, store_id, material_id, price_type, unit_price,
                       effective_from, effective_to, source_system
                FROM cost_material_price
                WHERE tenant_id = ? AND store_id = ? AND material_id = ? AND deleted = 0
                ORDER BY effective_from DESC
                """, (rs, rowNum) -> new PriceView(
                rs.getLong("id"), rs.getLong("store_id"), rs.getLong("material_id"),
                rs.getString("price_type"), rs.getBigDecimal("unit_price"),
                rs.getTimestamp("effective_from").toLocalDateTime(),
                rs.getTimestamp("effective_to") == null
                        ? null : rs.getTimestamp("effective_to").toLocalDateTime(),
                rs.getString("source_system")), tenantId, storeId, materialId);
    }

    @Transactional
    public Long createPrice(Long tenantId, Long userId, CreatePriceRequest request) {
        if (request.effectiveTo() != null && !request.effectiveTo().isAfter(request.effectiveFrom())) {
            throw new IllegalArgumentException("价格失效时间必须晚于生效时间");
        }
        requireStore(tenantId, request.storeId());
        requireMaterial(tenantId, request.materialId());
        Integer overlap = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM cost_material_price
                WHERE tenant_id = ? AND store_id = ? AND material_id = ? AND price_type = ?
                  AND deleted = 0
                  AND effective_from < COALESCE(?, '9999-12-31 23:59:59')
                  AND COALESCE(effective_to, '9999-12-31 23:59:59') > ?
                """, Integer.class, tenantId, request.storeId(), request.materialId(),
                request.priceType(), request.effectiveTo(), request.effectiveFrom());
        if (overlap != null && overlap > 0) {
            throw new IllegalArgumentException("同类型物料价格生效区间不能重叠");
        }
        return insertWithDuplicateMessage("""
                INSERT INTO cost_material_price
                  (tenant_id, store_id, material_id, price_type, unit_price,
                   effective_from, effective_to, source_system, created_by, updated_by)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, "该物料价格记录已存在", tenantId, request.storeId(), request.materialId(),
                request.priceType(), request.unitPrice(), request.effectiveFrom(), request.effectiveTo(),
                normalizeSource(request.sourceSystem()), userId, userId);
    }

    private Long insertWithDuplicateMessage(String sql, String duplicateMessage, Object... parameters) {
        GeneratedKeyHolder key = new GeneratedKeyHolder();
        try {
            jdbcTemplate.update(connection -> {
                PreparedStatement statement =
                        connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
                for (int index = 0; index < parameters.length; index++) {
                    statement.setObject(index + 1, parameters[index]);
                }
                return statement;
            }, key);
        } catch (DuplicateKeyException exception) {
            throw new IllegalArgumentException(duplicateMessage);
        }
        if (key.getKey() == null) {
            throw new IllegalStateException("新增记录失败");
        }
        return key.getKey().longValue();
    }

    private void requireUnit(Long tenantId, Long unitId) {
        requireOwned("cost_material_unit", tenantId, unitId, "单位");
    }

    private void requireMaterial(Long tenantId, Long materialId) {
        requireOwned("cost_material", tenantId, materialId, "物料");
    }

    private void requireStore(Long tenantId, Long storeId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_store WHERE tenant_id = ? AND id = ? AND status = 1",
                Integer.class, tenantId, storeId);
        if (count == null || count == 0) {
            throw new IllegalArgumentException("门店不存在或不属于当前商户");
        }
    }

    private void requireOwned(String table, Long tenantId, Long id, String label) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + table
                        + " WHERE tenant_id = ? AND id = ? AND status = 1 AND deleted = 0",
                Integer.class, tenantId, id);
        if (count == null || count == 0) {
            throw new IllegalArgumentException(label + "不存在、已停用或不属于当前商户");
        }
    }

    private String normalizeCode(String value) {
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeSource(String value) {
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
