package com.tql.store.cost.security;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class CostPermissionService {
    private final JdbcTemplate jdbcTemplate;

    public CostPermissionService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void require(Long userId, Long tenantId, String clientType, String permissionCode) {
        if (!"MERCHANT".equalsIgnoreCase(clientType)) {
            throw new SecurityException("当前端不允许执行该操作");
        }
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM sys_merchant_user_role ur
                JOIN sys_role role ON role.id = ur.role_id
                  AND role.tenant_id = ? AND role.client_type = 'MERCHANT' AND role.status = 1
                JOIN sys_merchant_user user ON user.id = ur.merchant_user_id
                  AND user.tenant_id = ? AND user.status = 1 AND user.login_enabled = 1 AND user.deleted = 0
                LEFT JOIN sys_role_menu rm ON rm.role_id = role.id
                LEFT JOIN sys_menu menu ON menu.id = rm.menu_id
                  AND menu.tenant_id = ? AND menu.client_type = 'MERCHANT'
                  AND menu.permission_code = ? AND menu.status = 1 AND menu.deleted = 0
                WHERE ur.merchant_user_id = ?
                  AND (UPPER(role.role_code) IN ('MERCHANT_ADMIN', 'ADMIN', 'SUPER_ADMIN') OR menu.id IS NOT NULL)
                """, Integer.class, tenantId, tenantId, tenantId, permissionCode, userId);
        if (count == null || count == 0) {
            throw new SecurityException("无权执行该操作");
        }
    }

    public void requireStoreAccess(Long userId, Long tenantId, Long storeId) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM sys_merchant_user user
                WHERE user.id = ?
                  AND user.tenant_id = ?
                  AND user.status = 1
                  AND user.login_enabled = 1
                  AND user.deleted = 0
                  AND EXISTS (
                      SELECT 1
                      FROM sys_merchant_organization store
                      WHERE store.id = ?
                        AND store.tenant_id = user.tenant_id
                        AND store.org_type = 'STORE'
                        AND store.status = 1
                        AND store.deleted = 0
                  )
                  AND (
                      EXISTS (
                          SELECT 1
                          FROM sys_merchant_user_role ur
                          JOIN sys_role role ON role.id = ur.role_id
                            AND role.tenant_id = user.tenant_id
                            AND role.client_type = 'MERCHANT'
                            AND role.status = 1
                            AND UPPER(role.role_code) IN ('MERCHANT_ADMIN', 'ADMIN', 'SUPER_ADMIN')
                          WHERE ur.merchant_user_id = user.id
                      )
                      OR EXISTS (
                          SELECT 1
                          FROM sys_merchant_user_store user_store
                          WHERE user_store.merchant_user_id = user.id
                            AND user_store.store_id = ?
                      )
                  )
                """, Integer.class, userId, tenantId, storeId, storeId);
        if (count == null || count == 0) {
            throw new SecurityException("无权访问该门店");
        }
    }
}
