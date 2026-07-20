package com.tql.store.integration.hualala;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public class HualalaShopRepository {

    private static final String UPSERT_SQL = """
            INSERT INTO integration_hll_shop
                (tenant_id, external_shop_id, external_group_id, external_shop_code,
                 shop_name, brand_id, brand_name, business_model, operation_mode, business_status,
                 city_code, city_name, address, shop_phone, shop_open_time, image_path,
                 longitude, latitude, record_action,
                 source_create_time, source_update_time, raw_json,
                 last_sync_task_id, last_sync_time, deleted, create_by, update_by)
            VALUES
                (:tenantId, :externalShopId, :externalGroupId, :externalShopCode,
                 :shopName, :brandId, :brandName, :businessModel, :operationMode, :businessStatus,
                 :cityCode, :cityName, :address, :shopPhone, :shopOpenTime, :imagePath,
                 :longitude, :latitude, :recordAction,
                 :sourceCreateTime, :sourceUpdateTime, CAST(:rawJson AS JSON),
                 :taskId, :syncTime, 0, :operatorId, :operatorId)
            ON DUPLICATE KEY UPDATE
                external_group_id = VALUES(external_group_id),
                external_shop_code = VALUES(external_shop_code),
                shop_name = VALUES(shop_name),
                brand_id = VALUES(brand_id),
                brand_name = VALUES(brand_name),
                business_model = VALUES(business_model),
                operation_mode = VALUES(operation_mode),
                business_status = VALUES(business_status),
                city_code = VALUES(city_code),
                city_name = VALUES(city_name),
                address = VALUES(address),
                shop_phone = VALUES(shop_phone),
                shop_open_time = VALUES(shop_open_time),
                image_path = VALUES(image_path),
                longitude = VALUES(longitude),
                latitude = VALUES(latitude),
                record_action = VALUES(record_action),
                source_create_time = VALUES(source_create_time),
                source_update_time = VALUES(source_update_time),
                raw_json = VALUES(raw_json),
                last_sync_task_id = VALUES(last_sync_task_id),
                last_sync_time = VALUES(last_sync_time),
                deleted = 0,
                update_by = VALUES(update_by)
            """;

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public HualalaShopRepository(NamedParameterJdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public int upsertAll(Long tenantId, Long taskId, List<HualalaShopRecord> shops) {
        if (shops == null || shops.isEmpty()) {
            return 0;
        }
        Long operatorId = jdbcTemplate.queryForObject("""
                SELECT created_by FROM integration_sync_task
                WHERE id = :taskId AND tenant_id = :tenantId
                """, new MapSqlParameterSource()
                .addValue("taskId", taskId)
                .addValue("tenantId", tenantId), Long.class);
        LocalDateTime syncTime = LocalDateTime.now();
        MapSqlParameterSource[] parameters = shops.stream()
                .map(shop -> toParameters(tenantId, taskId, operatorId, syncTime, shop))
                .toArray(MapSqlParameterSource[]::new);
        jdbcTemplate.batchUpdate(UPSERT_SQL, parameters);
        return shops.size();
    }

    private MapSqlParameterSource toParameters(
            Long tenantId, Long taskId, Long operatorId, LocalDateTime syncTime, HualalaShopRecord shop) {
        requireText(shop.externalShopId(), "哗啦啦门店ID不能为空");
        requireText(shop.shopName(), "哗啦啦门店名称不能为空");
        validateJson(shop.rawJson());
        return new MapSqlParameterSource()
                .addValue("tenantId", tenantId)
                .addValue("taskId", taskId)
                .addValue("operatorId", operatorId)
                .addValue("syncTime", syncTime)
                .addValue("externalShopId", shop.externalShopId())
                .addValue("externalGroupId", shop.externalGroupId())
                .addValue("externalShopCode", shop.externalShopCode())
                .addValue("shopName", shop.shopName())
                .addValue("brandId", shop.brandId())
                .addValue("brandName", shop.brandName())
                .addValue("businessModel", shop.businessModel())
                .addValue("operationMode", shop.operationMode())
                .addValue("businessStatus", shop.businessStatus())
                .addValue("cityCode", shop.cityCode())
                .addValue("cityName", shop.cityName())
                .addValue("address", shop.address())
                .addValue("shopPhone", shop.shopPhone())
                .addValue("shopOpenTime", shop.shopOpenTime())
                .addValue("imagePath", shop.imagePath())
                .addValue("longitude", shop.longitude())
                .addValue("latitude", shop.latitude())
                .addValue("recordAction", shop.recordAction())
                .addValue("sourceCreateTime", shop.sourceCreateTime())
                .addValue("sourceUpdateTime", shop.sourceUpdateTime())
                .addValue("rawJson", shop.rawJson());
    }

    private void validateJson(String rawJson) {
        requireText(rawJson, "哗啦啦门店原始数据不能为空");
        try {
            objectMapper.readTree(rawJson);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("哗啦啦门店原始数据不是合法 JSON", exception);
        }
    }

    private void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }
}
