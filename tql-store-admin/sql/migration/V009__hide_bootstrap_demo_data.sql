USE tql_store_saas;
SET NAMES utf8mb4;

SET @directory_visible_column_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'tql_store_saas'
      AND TABLE_NAME = 'sys_merchant_user'
      AND COLUMN_NAME = 'directory_visible'
);
SET @add_directory_visible_sql = IF(
    @directory_visible_column_exists = 0,
    'ALTER TABLE sys_merchant_user ADD COLUMN directory_visible TINYINT NOT NULL DEFAULT 1 COMMENT ''是否在用户目录显示：0隐藏，1显示'' AFTER login_enabled, ADD KEY idx_sys_merchant_user_directory (tenant_id, directory_visible, status, deleted)',
    'SELECT 1'
);
PREPARE add_directory_visible_statement FROM @add_directory_visible_sql;
EXECUTE add_directory_visible_statement;
DEALLOCATE PREPARE add_directory_visible_statement;

-- 引导账号只用于本地认证，不属于真实员工目录，也不关联虚构组织和门店。
UPDATE sys_merchant_user
SET organization_id = NULL,
    primary_store_id = NULL,
    email = NULL,
    phone = NULL,
    directory_visible = 0
WHERE tenant_id = 10001
  AND username = 'merchant_admin'
  AND source_type = 'LOCAL';

UPDATE sys_tenant
SET tenant_name = '同庆楼'
WHERE id = 10001
  AND tenant_code = 'TQL-DEMO'
  AND tenant_name = '同庆楼示范商家';

UPDATE sys_merchant_organization
SET status = 0, deleted = 1
WHERE tenant_id = 10001
  AND source_type = 'LOCAL'
  AND (id = 11001 OR org_code = 'TQL-HQ' OR org_name = '同庆楼示范商家总部');

UPDATE sys_store
SET status = 0
WHERE tenant_id = 10001
  AND id IN (11001, 11002, 11003, 11004)
  AND store_code IN ('TQL-HQ', 'TQL-BH', 'TQL-BH-LAKE', 'TQL-XZ');

DELETE FROM sys_merchant_user_store
WHERE merchant_user_id = 2
  AND store_id IN (11001, 11002, 11003, 11004);

UPDATE ops_content
SET deleted = 1
WHERE tenant_id = 10001
  AND id BETWEEN 1001 AND 1008
  AND create_by = 2;

SET @legacy_user_table = IF(
    EXISTS(SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA = 'tql_store_saas' AND TABLE_NAME = 'sys_user_legacy'),
    'sys_user_legacy',
    IF(EXISTS(SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA = 'tql_store_saas' AND TABLE_NAME = 'sys_user'), 'sys_user', NULL)
);
SET @clean_legacy_user_sql = IF(
    @legacy_user_table IS NULL,
    'SELECT 1',
    CONCAT('UPDATE ', @legacy_user_table,
           ' SET primary_store_id = NULL, email = NULL, phone = NULL WHERE tenant_id = 10001 AND username = ''merchant_admin'' AND client_type = ''MERCHANT''')
);
PREPARE clean_legacy_user_statement FROM @clean_legacy_user_sql;
EXECUTE clean_legacy_user_statement;
DEALLOCATE PREPARE clean_legacy_user_statement;
