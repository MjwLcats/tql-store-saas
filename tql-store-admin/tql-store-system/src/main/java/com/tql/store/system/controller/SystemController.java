package com.tql.store.system.controller;

import com.tql.store.common.api.ApiResponse;
import com.tql.store.system.model.MenuView;
import com.tql.store.system.model.UserProfile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/system")
public class SystemController {

    private final JdbcTemplate jdbcTemplate;

    public SystemController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/profile")
    public ApiResponse<UserProfile> profile(
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-Tenant-Id") Long tenantId,
            @RequestHeader("X-Client-Type") String clientType) {
        boolean merchant = "MERCHANT".equalsIgnoreCase(clientType);
        String sql = merchant ? """
                SELECT u.id, u.tenant_id, t.tenant_name,
                       u.primary_store_id, s.store_name AS primary_store_name, u.data_scope,
                       u.username, u.display_name, u.email, u.phone, 'MERCHANT' AS client_type
                FROM sys_merchant_user u
                JOIN sys_tenant t ON t.id = u.tenant_id AND t.status = 1
                LEFT JOIN sys_store s ON s.id = u.primary_store_id AND s.tenant_id = u.tenant_id
                WHERE u.id = ? AND u.tenant_id = ? AND u.status = 1
                  AND u.login_enabled = 1 AND u.deleted = 0
                """ : """
                SELECT u.id, 0 AS tenant_id, 'SaaS 平台' AS tenant_name,
                       NULL AS primary_store_id, NULL AS primary_store_name, u.data_scope,
                       u.username, u.display_name, u.email, u.phone, 'PLATFORM' AS client_type
                FROM sys_platform_user u
                WHERE u.id = ? AND u.status = 1 AND u.deleted = 0
                """;
        Object[] params = merchant ? new Object[]{userId, tenantId} : new Object[]{userId};
        UserProfile profile = jdbcTemplate.queryForObject(sql, (rs, rowNum) -> new UserProfile(
                rs.getLong("id"),
                rs.getLong("tenant_id"),
                rs.getString("tenant_name"),
                rs.getObject("primary_store_id", Long.class),
                rs.getString("primary_store_name"),
                rs.getString("data_scope"),
                rs.getString("username"),
                rs.getString("display_name"),
                rs.getString("email"),
                rs.getString("phone"),
                rs.getString("client_type")
        ), params);
        return ApiResponse.success(profile);
    }

    @GetMapping("/menus")
    public ApiResponse<List<MenuView>> menus(
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-Client-Type") String clientType,
            @RequestHeader("X-Tenant-Id") Long tenantId) {
        boolean merchant = "MERCHANT".equalsIgnoreCase(clientType);
        String userRoleTable = merchant ? "sys_merchant_user_role" : "sys_platform_user_role";
        String userIdColumn = merchant ? "merchant_user_id" : "platform_user_id";
        String sql = """
                SELECT DISTINCT m.id, m.parent_id, m.menu_name, m.menu_type, m.route_name,
                       m.route_path, m.component_key, m.icon, m.icon_id, i.svg_content AS icon_svg, m.permission_code,
                       m.sort_order, m.visible, m.status
                FROM sys_menu m
                JOIN sys_role_menu rm ON rm.menu_id = m.id
                LEFT JOIN sys_icon i ON i.id = m.icon_id
                JOIN %s ur ON ur.role_id = rm.role_id
                JOIN sys_role r ON r.id = ur.role_id AND r.status = 1
                WHERE ur.%s = ? AND m.client_type = ? AND m.tenant_id = ?
                  AND m.deleted = 0
                ORDER BY m.sort_order, m.id
                """.formatted(userRoleTable, userIdColumn);
        List<MenuView> menus = jdbcTemplate.query(sql, (rs, rowNum) -> new MenuView(
                rs.getLong("id"),
                rs.getLong("parent_id"),
                rs.getString("menu_name"),
                rs.getString("menu_type"),
                rs.getString("route_name"),
                rs.getString("route_path"),
                rs.getString("component_key"),
                rs.getString("icon"),
                rs.getObject("icon_id", Long.class),
                rs.getString("icon_svg"),
                rs.getString("permission_code"),
                rs.getInt("sort_order"),
                rs.getInt("visible"),
                rs.getInt("status")
        ), userId, clientType, tenantId);
        return ApiResponse.success(menus);
    }
}
