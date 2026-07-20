package com.tql.store.operation.controller;

import com.tql.store.common.api.ApiResponse;
import com.tql.store.common.api.PageResult;
import com.tql.store.operation.model.ContentView;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/api/operation/contents")
public class ContentController {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public ContentController(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping
    public ApiResponse<PageResult<ContentView>> list(
            @RequestHeader("X-Tenant-Id") Long tenantId,
            @RequestHeader("X-Client-Type") String clientType,
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {

        int safePage = Math.max(page, 1);
        int safePageSize = Math.min(Math.max(pageSize, 1), 100);
        StringBuilder where = new StringBuilder(" WHERE c.deleted = 0 ");
        MapSqlParameterSource params = new MapSqlParameterSource();

        if (!"PLATFORM".equalsIgnoreCase(clientType)) {
            where.append(" AND c.tenant_id = :tenantId ");
            params.addValue("tenantId", tenantId);
            UserScope userScope = resolveUserScope(userId, tenantId, clientType);
            applyDataScope(where, params, tenantId, userId,
                    userScope.primaryStoreId(), userScope.dataScope());
        }
        if (keyword != null && !keyword.isBlank()) {
            where.append(" AND c.title LIKE :keyword ");
            params.addValue("keyword", "%" + keyword.trim() + "%");
        }
        if (category != null && !category.isBlank()) {
            where.append(" AND c.category = :category ");
            params.addValue("category", category.trim());
        }
        if (status != null && !status.isBlank()) {
            where.append(" AND c.status = :status ");
            params.addValue("status", status.trim());
        }

        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ops_content c" + where,
                params,
                Long.class
        );

        params.addValue("offset", (safePage - 1) * safePageSize);
        params.addValue("pageSize", safePageSize);
        List<ContentView> records = jdbcTemplate.query("""
                SELECT c.id, c.tenant_id, c.store_id, s.store_name,
                       c.title, c.category, c.status, c.owner, c.publish_time
                FROM ops_content c
                LEFT JOIN sys_store s ON s.id = c.store_id AND s.tenant_id = c.tenant_id
                """ + where + " ORDER BY COALESCE(c.publish_time, c.create_time) DESC, c.id DESC LIMIT :offset, :pageSize",
                params,
                (rs, rowNum) -> new ContentView(
                        rs.getLong("id"),
                        rs.getLong("tenant_id"),
                        rs.getObject("store_id", Long.class),
                        rs.getString("store_name"),
                        rs.getString("title"),
                        rs.getString("category"),
                        rs.getString("status"),
                        rs.getString("owner"),
                        rs.getTimestamp("publish_time") == null
                                ? null : rs.getTimestamp("publish_time").toLocalDateTime()
                )
        );

        return ApiResponse.success(new PageResult<>(records, total == null ? 0 : total, safePage, safePageSize));
    }

    private void applyDataScope(
            StringBuilder where,
            MapSqlParameterSource params,
            Long tenantId,
            Long userId,
            Long storeId,
            String dataScope) {
        String scope = dataScope == null ? "SELF" : dataScope.trim().toUpperCase(Locale.ROOT);
        switch (scope) {
            case "ALL" -> {
                // 当前租户全部数据，租户条件已在主查询中添加。
            }
            case "STORE" -> appendStoreFilter(where, params, storeId == null ? List.of() : List.of(storeId));
            case "STORE_AND_CHILD" -> appendStoreFilter(
                    where, params, storeId == null ? List.of() : descendantStoreIds(tenantId, storeId));
            case "CUSTOM" -> appendStoreFilter(where, params, jdbcTemplate.query("""
                    SELECT us.store_id
                    FROM sys_merchant_user_store us
                    JOIN sys_store s ON s.id = us.store_id AND s.tenant_id = :tenantId AND s.status = 1
                    WHERE us.merchant_user_id = :userId
                    """, new MapSqlParameterSource("tenantId", tenantId).addValue("userId", userId),
                    (rs, rowNum) -> rs.getLong("store_id")));
            case "SELF" -> {
                where.append(" AND c.create_by = :scopeUserId ");
                params.addValue("scopeUserId", userId);
            }
            default -> where.append(" AND 1 = 0 ");
        }
    }

    private UserScope resolveUserScope(Long userId, Long tenantId, String clientType) {
        List<UserScope> users = jdbcTemplate.query("""
                SELECT primary_store_id, data_scope
                FROM sys_merchant_user
                WHERE id = :userId AND tenant_id = :tenantId
                  AND status = 1 AND login_enabled = 1 AND deleted = 0
                """, new MapSqlParameterSource("userId", userId)
                .addValue("tenantId", tenantId),
                (rs, rowNum) -> new UserScope(
                        rs.getObject("primary_store_id", Long.class), rs.getString("data_scope")));
        if (users.isEmpty()) throw new SecurityException("当前用户已停用或不存在");
        return users.get(0);
    }

    private List<Long> descendantStoreIds(Long tenantId, Long storeId) {
        return jdbcTemplate.query("""
                WITH RECURSIVE store_tree AS (
                    SELECT id FROM sys_store WHERE id = :storeId AND tenant_id = :tenantId AND status = 1
                    UNION ALL
                    SELECT s.id FROM sys_store s
                    JOIN store_tree parent ON s.parent_id = parent.id
                    WHERE s.tenant_id = :tenantId AND s.status = 1
                )
                SELECT id FROM store_tree
                """, new MapSqlParameterSource("tenantId", tenantId).addValue("storeId", storeId),
                (rs, rowNum) -> rs.getLong("id"));
    }

    private void appendStoreFilter(
            StringBuilder where, MapSqlParameterSource params, List<Long> storeIds) {
        if (storeIds.isEmpty()) {
            where.append(" AND 1 = 0 ");
            return;
        }
        where.append(" AND c.store_id IN (:scopeStoreIds) ");
        params.addValue("scopeStoreIds", storeIds);
    }

    private record UserScope(Long primaryStoreId, String dataScope) {
    }
}
