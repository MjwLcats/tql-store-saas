USE tql_store_saas;
SET NAMES utf8mb4;

SET @primary_store_column_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'tql_store_saas'
      AND TABLE_NAME = 'sys_merchant_user'
      AND COLUMN_NAME = 'primary_store_id'
);
SET @add_primary_store_sql = IF(
    @primary_store_column_exists = 0,
    'ALTER TABLE sys_merchant_user ADD COLUMN primary_store_id BIGINT DEFAULT NULL COMMENT ''用户主要所属门店ID'' AFTER organization_id, ADD KEY idx_sys_merchant_user_primary_store (tenant_id, primary_store_id)',
    'SELECT 1'
);
PREPARE add_primary_store_statement FROM @add_primary_store_sql;
EXECUTE add_primary_store_statement;
DEALLOCATE PREPARE add_primary_store_statement;

CREATE TABLE IF NOT EXISTS sys_platform_user_role (
    platform_user_id BIGINT NOT NULL COMMENT '平台用户ID',
    role_id BIGINT NOT NULL COMMENT '平台角色ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (platform_user_id, role_id),
    KEY idx_platform_user_role_role (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='平台用户角色关联表';

CREATE TABLE IF NOT EXISTS sys_merchant_user_role (
    merchant_user_id BIGINT NOT NULL COMMENT '商家用户ID',
    role_id BIGINT NOT NULL COMMENT '商家角色ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (merchant_user_id, role_id),
    KEY idx_merchant_user_role_role (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商家用户角色关联表';

CREATE TABLE IF NOT EXISTS sys_merchant_user_store (
    merchant_user_id BIGINT NOT NULL COMMENT '商家用户ID',
    store_id BIGINT NOT NULL COMMENT '授权门店ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (merchant_user_id, store_id),
    KEY idx_merchant_user_store_store (store_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商家用户授权门店关联表';

SET @user_source_table = IF(
    EXISTS(SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA = 'tql_store_saas' AND TABLE_NAME = 'sys_user'),
    'sys_user', 'sys_user_legacy'
);
SET @user_role_source_table = IF(
    EXISTS(SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA = 'tql_store_saas' AND TABLE_NAME = 'sys_user_role'),
    'sys_user_role', 'sys_user_role_legacy'
);
SET @user_store_source_table = IF(
    EXISTS(SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA = 'tql_store_saas' AND TABLE_NAME = 'sys_user_store'),
    'sys_user_store', 'sys_user_store_legacy'
);
SET @create_user_source_view_sql = CONCAT(
    'CREATE OR REPLACE VIEW migration_v008_user_source AS SELECT * FROM ', @user_source_table
);
PREPARE create_user_source_view_statement FROM @create_user_source_view_sql;
EXECUTE create_user_source_view_statement;
DEALLOCATE PREPARE create_user_source_view_statement;
SET @create_user_role_source_view_sql = CONCAT(
    'CREATE OR REPLACE VIEW migration_v008_user_role_source AS SELECT * FROM ', @user_role_source_table
);
PREPARE create_user_role_source_view_statement FROM @create_user_role_source_view_sql;
EXECUTE create_user_role_source_view_statement;
DEALLOCATE PREPARE create_user_role_source_view_statement;
SET @create_user_store_source_view_sql = CONCAT(
    'CREATE OR REPLACE VIEW migration_v008_user_store_source AS SELECT * FROM ', @user_store_source_table
);
PREPARE create_user_store_source_view_statement FROM @create_user_store_source_view_sql;
EXECUTE create_user_store_source_view_statement;
DEALLOCATE PREPARE create_user_store_source_view_statement;

UPDATE sys_platform_user target
JOIN migration_v008_user_source source
  ON source.id = target.id AND source.client_type = 'PLATFORM'
SET target.username = source.username,
    target.password_hash = source.password_hash,
    target.display_name = source.display_name,
    target.email = source.email,
    target.phone = source.phone,
    target.data_scope = source.data_scope,
    target.status = source.status,
    target.deleted = 0;

UPDATE sys_merchant_user target
JOIN migration_v008_user_source source
  ON source.tenant_id = target.tenant_id
 AND source.username = target.username
 AND source.client_type = 'MERCHANT'
SET target.primary_store_id = source.primary_store_id,
    target.password_hash = source.password_hash,
    target.display_name = source.display_name,
    target.email = source.email,
    target.phone = source.phone,
    target.login_enabled = 1,
    target.data_scope = source.data_scope,
    target.status = source.status,
    target.deleted = 0;

INSERT IGNORE INTO sys_platform_user_role (platform_user_id, role_id)
SELECT platform_user.id, relation.role_id
FROM sys_platform_user platform_user
JOIN migration_v008_user_source source_user
  ON source_user.id = platform_user.id AND source_user.client_type = 'PLATFORM'
JOIN migration_v008_user_role_source relation ON relation.user_id = source_user.id
JOIN sys_role role
  ON role.id = relation.role_id AND role.client_type = 'PLATFORM' AND role.tenant_id = 0;

INSERT IGNORE INTO sys_merchant_user_role (merchant_user_id, role_id)
SELECT merchant_user.id, relation.role_id
FROM sys_merchant_user merchant_user
JOIN migration_v008_user_source source_user
  ON source_user.tenant_id = merchant_user.tenant_id
 AND source_user.username = merchant_user.username
 AND source_user.client_type = 'MERCHANT'
JOIN migration_v008_user_role_source relation ON relation.user_id = source_user.id
JOIN sys_role role
  ON role.id = relation.role_id
 AND role.client_type = 'MERCHANT'
 AND role.tenant_id = merchant_user.tenant_id;

INSERT IGNORE INTO sys_merchant_user_store (merchant_user_id, store_id)
SELECT merchant_user.id, relation.store_id
FROM sys_merchant_user merchant_user
JOIN migration_v008_user_source source_user
  ON source_user.tenant_id = merchant_user.tenant_id
 AND source_user.username = merchant_user.username
 AND source_user.client_type = 'MERCHANT'
JOIN migration_v008_user_store_source relation ON relation.user_id = source_user.id
JOIN sys_store store_record
  ON store_record.id = relation.store_id AND store_record.tenant_id = merchant_user.tenant_id;

DROP VIEW migration_v008_user_source;
DROP VIEW migration_v008_user_role_source;
DROP VIEW migration_v008_user_store_source;

SET @rename_user_sql = IF(
    EXISTS(SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA = 'tql_store_saas' AND TABLE_NAME = 'sys_user'),
    IF(
        EXISTS(SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA = 'tql_store_saas' AND TABLE_NAME = 'sys_user_legacy'),
        'DROP TABLE sys_user',
        'RENAME TABLE sys_user TO sys_user_legacy'
    ), 'SELECT 1'
);
PREPARE rename_user_statement FROM @rename_user_sql;
EXECUTE rename_user_statement;
DEALLOCATE PREPARE rename_user_statement;

SET @rename_user_role_sql = IF(
    EXISTS(SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA = 'tql_store_saas' AND TABLE_NAME = 'sys_user_role'),
    IF(
        EXISTS(SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA = 'tql_store_saas' AND TABLE_NAME = 'sys_user_role_legacy'),
        'DROP TABLE sys_user_role',
        'RENAME TABLE sys_user_role TO sys_user_role_legacy'
    ), 'SELECT 1'
);
PREPARE rename_user_role_statement FROM @rename_user_role_sql;
EXECUTE rename_user_role_statement;
DEALLOCATE PREPARE rename_user_role_statement;

SET @rename_user_store_sql = IF(
    EXISTS(SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA = 'tql_store_saas' AND TABLE_NAME = 'sys_user_store'),
    IF(
        EXISTS(SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA = 'tql_store_saas' AND TABLE_NAME = 'sys_user_store_legacy'),
        'DROP TABLE sys_user_store',
        'RENAME TABLE sys_user_store TO sys_user_store_legacy'
    ), 'SELECT 1'
);
PREPARE rename_user_store_statement FROM @rename_user_store_sql;
EXECUTE rename_user_store_statement;
DEALLOCATE PREPARE rename_user_store_statement;

ALTER TABLE sys_user_legacy COMMENT = '历史统一用户表，仅用于迁移审计，运行时代码禁止访问';
ALTER TABLE sys_user_role_legacy COMMENT = '历史用户角色关联表，仅用于迁移审计，运行时代码禁止访问';
ALTER TABLE sys_user_store_legacy COMMENT = '历史用户门店关联表，仅用于迁移审计，运行时代码禁止访问';
