package com.tql.store.integration.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class IntegrationPermissionService {

    private final JdbcTemplate jdbcTemplate;

    public IntegrationPermissionService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void requireSyncPermission(Long userId, Long tenantId, String clientType) {
        if (!"MERCHANT".equalsIgnoreCase(clientType)) {
            throw new SecurityException("第三方数据同步仅支持商家端操作");
        }
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM sys_merchant_user_role ur
                JOIN sys_role r ON r.id = ur.role_id AND r.status = 1
                JOIN sys_role_menu rm ON rm.role_id = r.id
                JOIN sys_menu m ON m.id = rm.menu_id AND m.visible = 1
                WHERE ur.merchant_user_id = ? AND r.tenant_id = ? AND r.client_type = 'MERCHANT'
                  AND m.permission_code = 'merchant:integration:sync:view'
                """, Integer.class, userId, tenantId);
        if (count == null || count == 0) {
            throw new SecurityException("无权执行第三方数据同步操作");
        }
    }
}
