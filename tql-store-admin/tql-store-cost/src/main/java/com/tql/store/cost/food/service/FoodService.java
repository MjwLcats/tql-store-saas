package com.tql.store.cost.food.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.tql.store.cost.food.model.FoodView;
import com.tql.store.cost.food.model.PullFoodRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class FoodService {
    private final JdbcTemplate jdbcTemplate;
    private final RestClient legacyClient;
    private final String legacyFoodPath;
    private final String legacySyncFoodPath;

    public FoodService(
            JdbcTemplate jdbcTemplate,
            @Value("${legacy-cost.base-url:http://127.0.0.1:9304}") String legacyBaseUrl,
            @Value("${legacy-cost.food-path:/bom/food/findAllFood}") String legacyFoodPath,
            @Value("${legacy-cost.sync-food-path:/bom/food/findSyncFoodList}") String legacySyncFoodPath) {
        if (!legacyBaseUrl.matches("https?://(127\\.0\\.0\\.1|localhost)(:\\d+)?")) {
            throw new IllegalArgumentException("旧成本接口只允许配置为本机地址");
        }
        this.jdbcTemplate = jdbcTemplate;
        this.legacyClient = RestClient.create(legacyBaseUrl);
        this.legacyFoodPath = legacyFoodPath;
        this.legacySyncFoodPath = legacySyncFoodPath;
    }

    public Map<String, Object> syncCandidates(Long shopId, String foodCode, String foodName,
                                               int pageNum, int pageSize, String authorization) {
        JsonNode body = readLegacyPage(shopId, foodCode, foodName, pageNum, pageSize, authorization);
        List<JsonNode> rows = new ArrayList<>();
        if (body != null && body.path("rows").isArray()) body.path("rows").forEach(rows::add);
        long total = body == null ? 0 : body.path("total").asLong(rows.size());
        return Map.of("rows", rows, "total", total);
    }

    public JsonNode sourceShops(String authorization) {
        JsonNode body = legacyClient.get().uri("/bom/shop/find")
                .headers(headers -> {
                    if (authorization != null && !authorization.isBlank()) headers.set("Authorization", authorization);
                })
                .retrieve().body(JsonNode.class);
        if (body == null) return com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.arrayNode();
        if (body.path("data").isArray()) return body.path("data");
        if (body.path("rows").isArray()) return body.path("rows");
        return body;
    }

    @Transactional
    public int saveSelected(Long tenantId, List<JsonNode> rows) {
        for (JsonNode row : rows) upsert(tenantId, row);
        return rows.size();
    }

    @Transactional
    public int saveAll(Long tenantId, Long shopId, String foodCode, String foodName, String authorization) {
        int page = 1;
        int saved = 0;
        while (true) {
            JsonNode body = readLegacyPage(shopId, foodCode, foodName, page, 500, authorization);
            JsonNode rows = body == null ? null : body.path("rows");
            if (rows == null || !rows.isArray() || rows.isEmpty()) break;
            for (JsonNode row : rows) {
                upsert(tenantId, row);
                saved++;
            }
            long total = body.path("total").asLong(saved);
            if (saved >= total || rows.size() < 500) break;
            page++;
        }
        return saved;
    }

    private JsonNode readLegacyPage(Long shopId, String foodCode, String foodName,
                                    int pageNum, int pageSize, String authorization) {
        var uri = UriComponentsBuilder.fromPath(legacySyncFoodPath)
                .queryParam("shopId", shopId)
                .queryParamIfPresent("foodCode", java.util.Optional.ofNullable(foodCode))
                .queryParamIfPresent("foodName", java.util.Optional.ofNullable(foodName))
                .queryParam("pageNum", pageNum)
                .queryParam("pageSize", pageSize)
                .build().encode().toUriString();
        return legacyClient.get().uri(uri)
                .headers(headers -> {
                    if (authorization != null && !authorization.isBlank()) {
                        headers.set("Authorization", authorization);
                    }
                })
                .retrieve().body(JsonNode.class);
    }

    public List<FoodView> list(Long tenantId, Long shopId, String keyword) {
        String like = "%" + (keyword == null ? "" : keyword.trim()) + "%";
        return jdbcTemplate.query("""
                SELECT id, foodID, shopID, foodCode, foodName, foodCategoryCode,
                       foodCategoryName, foodMnemonicCode, isActive, isOpen, origin, tagId, areaTagName
                FROM tql_food
                WHERE tenant_id=? AND shopID=? AND dr='0'
                  AND (?='' OR foodCode LIKE ? OR foodName LIKE ? OR foodMnemonicCode LIKE ?)
                ORDER BY foodCategoryName, foodName
                """, (rs, rowNum) -> new FoodView(
                rs.getLong("id"), rs.getLong("foodID"), rs.getLong("shopID"),
                rs.getString("foodCode"), rs.getString("foodName"),
                rs.getString("foodCategoryCode"), rs.getString("foodCategoryName"),
                rs.getString("foodMnemonicCode"), rs.getString("isActive"),
                rs.getString("isOpen"), rs.getString("origin"),
                rs.getString("tagId"), rs.getString("areaTagName")
        ), tenantId, shopId, keyword == null ? "" : keyword.trim(), like, like, like);
    }

    @Transactional
    public int pull(Long tenantId, PullFoodRequest request, String authorization) {
        var uri = UriComponentsBuilder.fromPath(legacyFoodPath)
                .queryParam("shopId", request.shopId())
                .queryParamIfPresent("deptCode", java.util.Optional.ofNullable(request.deptCode()))
                .queryParamIfPresent("foodCode", java.util.Optional.ofNullable(request.foodCode()))
                .queryParamIfPresent("foodName", java.util.Optional.ofNullable(request.foodName()))
                .queryParam("pageNum", 1)
                .queryParam("pageSize", 10000)
                .build().encode().toUriString();
        JsonNode body = legacyClient.get().uri(uri)
                .headers(headers -> {
                    if (authorization != null && !authorization.isBlank()) {
                        headers.set("Authorization", authorization);
                    }
                })
                .retrieve().body(JsonNode.class);
        List<JsonNode> rows = new ArrayList<>();
        if (body != null && body.path("rows").isArray()) body.path("rows").forEach(rows::add);
        for (JsonNode row : rows) upsert(tenantId, row);
        return rows.size();
    }

    private void upsert(Long tenantId, JsonNode row) {
        jdbcTemplate.update("""
                INSERT INTO tql_food
                  (tenant_id,isActive,foodCode,foodName,foodCategoryKey,foodCategoryCode,
                   foodCategoryName,foodType,foodID,groupID,shopID,departmentID,parentFoodID,
                   isOpen,isNews,isTempFood,isSetFood,foodMnemonicCode,origin,tagId,areaTagName,actionTime,dr,sync_time)
                VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?, '0',NOW())
                ON DUPLICATE KEY UPDATE foodCode=VALUES(foodCode),foodName=VALUES(foodName),
                  isActive=VALUES(isActive),foodCategoryKey=VALUES(foodCategoryKey),
                  foodCategoryCode=VALUES(foodCategoryCode),foodCategoryName=VALUES(foodCategoryName),
                  foodType=VALUES(foodType),groupID=VALUES(groupID),departmentID=VALUES(departmentID),
                  parentFoodID=VALUES(parentFoodID),isOpen=VALUES(isOpen),isNews=VALUES(isNews),
                  isTempFood=VALUES(isTempFood),isSetFood=VALUES(isSetFood),
                  foodMnemonicCode=VALUES(foodMnemonicCode),origin=VALUES(origin),tagId=VALUES(tagId),
                  areaTagName=VALUES(areaTagName),actionTime=VALUES(actionTime),dr='0',sync_time=NOW()
                """, tenantId, text(row,"isActive"), text(row,"foodCode"), text(row,"foodName"),
                text(row,"foodCategoryKey"), text(row,"foodCategoryCode"), text(row,"foodCategoryName"),
                text(row,"foodType"), number(row,"foodId","foodID"), text(row,"groupId","groupID"),
                number(row,"shopId","shopID"), text(row,"departmentId","departmentID"),
                text(row,"parentFoodId","parentFoodID"), text(row,"isOpen"), text(row,"isNew","isNews"),
                text(row,"isTempFood"), text(row,"isSetFood"),
                text(row,"foodMnemonicCode"), text(row,"origin"), text(row,"tagId"),
                text(row,"areaTagName"), text(row,"actionTime"));
    }

    private String text(JsonNode row, String... names) {
        for (String name : names) if (row.hasNonNull(name)) return row.get(name).asText();
        return null;
    }

    private Long number(JsonNode row, String... names) {
        for (String name : names) if (row.hasNonNull(name)) return row.get(name).asLong();
        throw new IllegalArgumentException("food 接口缺少必要主键字段");
    }
}
