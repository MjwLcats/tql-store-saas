package com.tql.store.operation.content.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class ContentPermissionService {

    private final JdbcTemplate jdbcTemplate;

    public ContentPermissionService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void require(Long userId, Long tenantId, String clientType, String permissionCode) {
        if (!"MERCHANT".equalsIgnoreCase(clientType)) {
            throw new SecurityException("当前端不允许执行该操作");
        }
        Integer adminCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM sys_merchant_user_role ur
                JOIN sys_role role
                  ON role.id = ur.role_id
                 AND role.tenant_id = ?
                 AND role.client_type = 'MERCHANT'
                 AND role.role_code = 'MERCHANT_ADMIN'
                 AND role.status = 1
                JOIN sys_merchant_user user
                  ON user.id = ur.merchant_user_id
                 AND user.tenant_id = ?
                 AND user.status = 1
                 AND user.login_enabled = 1
                 AND user.deleted = 0
                WHERE ur.merchant_user_id = ?
                """, Integer.class, tenantId, tenantId, userId);
        if (adminCount != null && adminCount > 0) {
            return;
        }
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM sys_merchant_user_role ur
                JOIN sys_role role
                  ON role.id = ur.role_id
                 AND role.tenant_id = ?
                 AND role.client_type = 'MERCHANT'
                 AND role.status = 1
                JOIN sys_role_menu rm ON rm.role_id = role.id
                JOIN sys_menu menu
                  ON menu.id = rm.menu_id
                 AND menu.tenant_id = ?
                 AND menu.client_type = 'MERCHANT'
                 AND menu.permission_code = ?
                 AND menu.status = 1
                 AND menu.deleted = 0
                JOIN sys_merchant_user user
                  ON user.id = ur.merchant_user_id
                 AND user.tenant_id = ?
                 AND user.status = 1
                 AND user.login_enabled = 1
                 AND user.deleted = 0
                WHERE ur.merchant_user_id = ?
                """, Integer.class, tenantId, tenantId, permissionCode, tenantId, userId);
        if (count == null || count == 0) {
            throw new SecurityException("无权执行该操作");
        }
    }

    public void requireAny(Long userId, Long tenantId, String clientType, String... permissionCodes) {
        SecurityException lastException = null;
        for (String permissionCode : permissionCodes) {
            try {
                require(userId, tenantId, clientType, permissionCode);
                return;
            } catch (SecurityException exception) {
                lastException = exception;
            }
        }
        throw lastException == null ? new SecurityException("No permission to perform this operation") : lastException;
    }
}
