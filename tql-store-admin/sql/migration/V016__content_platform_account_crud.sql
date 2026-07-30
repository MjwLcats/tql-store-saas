USE tql_store_saas;

CREATE TABLE IF NOT EXISTS ops_content_platform_account (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    platform VARCHAR(32) NOT NULL,
    account_name VARCHAR(128) NOT NULL,
    platform_account_id VARCHAR(128) NOT NULL,
    account_type VARCHAR(32) NOT NULL,
    organization_id BIGINT DEFAULT NULL,
    employee_id BIGINT DEFAULT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    create_by BIGINT DEFAULT NULL,
    update_by BIGINT DEFAULT NULL,
    create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_content_account (tenant_id, platform, platform_account_id, deleted),
    KEY idx_content_account_employee (tenant_id, employee_id),
    KEY idx_content_account_organization (tenant_id, organization_id),
    KEY idx_content_account_status (tenant_id, status)
) COMMENT='AI内容中心平台账号';

UPDATE sys_menu
SET menu_name = CASE permission_code
    WHEN 'merchant:content:account:import' THEN '导入账号'
    WHEN 'merchant:content:account:export' THEN '下载账号'
    ELSE menu_name
END
WHERE client_type = 'MERCHANT'
  AND permission_code IN (
      'merchant:content:account:import',
      'merchant:content:account:export'
  )
  AND deleted = 0;

INSERT INTO sys_menu
    (tenant_id, parent_id, menu_name, menu_type, route_name, route_path, component_key,
     icon, permission_code, client_type, sort_order, visible, status, system_builtin, deleted)
SELECT parent.tenant_id, parent.id, item.menu_name, 'BUTTON', NULL, NULL, NULL,
       NULL, item.permission_code, 'MERCHANT', item.sort_order, 0, 1, 0, 0
FROM sys_menu parent
JOIN (
    SELECT '新增账号' menu_name, 'merchant:content:account:create' permission_code, 15 sort_order
    UNION ALL SELECT '编辑账号', 'merchant:content:account:update', 16
    UNION ALL SELECT '删除账号', 'merchant:content:account:delete', 17
) item
WHERE parent.client_type = 'MERCHANT'
  AND parent.route_name = 'MerchantContentAccounts'
  AND parent.deleted = 0
  AND NOT EXISTS (
      SELECT 1 FROM sys_menu existing
      WHERE existing.tenant_id = parent.tenant_id
        AND existing.client_type = 'MERCHANT'
        AND existing.permission_code = item.permission_code
        AND existing.deleted = 0
  );

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT role.id, menu.id
FROM sys_role role
JOIN sys_menu menu
  ON menu.tenant_id = role.tenant_id
 AND menu.client_type = 'MERCHANT'
 AND menu.permission_code IN (
     'merchant:content:account:create',
     'merchant:content:account:update',
     'merchant:content:account:delete'
 )
 AND menu.deleted = 0
WHERE role.client_type = 'MERCHANT'
  AND role.role_code = 'MERCHANT_ADMIN'
  AND role.status = 1;
