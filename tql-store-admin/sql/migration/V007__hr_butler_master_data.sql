USE tql_store_saas;
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS integration_rlgj_organization (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    tenant_id BIGINT NOT NULL COMMENT '租户ID',
    external_org_id VARCHAR(64) NOT NULL COMMENT '人力管家组织ID',
    external_parent_id VARCHAR(64) DEFAULT NULL COMMENT '人力管家上级组织ID',
    org_code VARCHAR(64) DEFAULT NULL COMMENT '人力管家组织编码',
    org_name VARCHAR(160) NOT NULL COMMENT '组织名称',
    org_type VARCHAR(32) NOT NULL COMMENT '组织类型：company公司、dept部门、store门店、area区域',
    store_code VARCHAR(64) DEFAULT NULL COMMENT '门店编码，仅门店类型组织有值',
    lead_user_id VARCHAR(64) DEFAULT NULL COMMENT '组织负责人在人力管家的用户ID',
    lead_name VARCHAR(64) DEFAULT NULL COMMENT '组织负责人姓名',
    valid_status TINYINT NOT NULL DEFAULT 1 COMMENT '人力管家组织状态：0删除，1正常',
    brand_id VARCHAR(64) DEFAULT NULL COMMENT '品牌ID，仅门店类型组织可能有值',
    brand_name VARCHAR(160) DEFAULT NULL COMMENT '品牌名称',
    province VARCHAR(64) DEFAULT NULL COMMENT '所在省份名称',
    city VARCHAR(64) DEFAULT NULL COMMENT '所在城市名称',
    district VARCHAR(64) DEFAULT NULL COMMENT '所在区县名称',
    org_address VARCHAR(255) DEFAULT NULL COMMENT '组织详细地址',
    cost_org_code VARCHAR(64) DEFAULT NULL COMMENT '成本组织编码',
    corporation_code VARCHAR(64) DEFAULT NULL COMMENT '法人公司编码',
    corporation_name VARCHAR(160) DEFAULT NULL COMMENT '法人公司名称',
    super_lead_user_id VARCHAR(64) DEFAULT NULL COMMENT '分管领导在人力管家的用户ID',
    source_create_time DATETIME DEFAULT NULL COMMENT '人力管家记录创建时间',
    source_update_time DATETIME DEFAULT NULL COMMENT '人力管家记录更新时间',
    raw_json JSON NOT NULL COMMENT '经过字段筛选后的第三方组织JSON数据',
    last_sync_task_id BIGINT NOT NULL COMMENT '最后同步任务ID',
    last_sync_time DATETIME NOT NULL COMMENT '最后同步时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标志：0未删除，1已删除',
    create_by BIGINT DEFAULT NULL COMMENT '创建人用户ID',
    update_by BIGINT DEFAULT NULL COMMENT '更新人用户ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_integration_rlgj_org (tenant_id, external_org_id),
    KEY idx_integration_rlgj_org_parent (tenant_id, external_parent_id),
    KEY idx_integration_rlgj_org_type (tenant_id, org_type, valid_status, deleted),
    KEY idx_integration_rlgj_org_task (tenant_id, last_sync_task_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='人力管家组织数据镜像表';

CREATE TABLE IF NOT EXISTS integration_rlgj_position (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    tenant_id BIGINT NOT NULL COMMENT '租户ID',
    external_position_id VARCHAR(64) NOT NULL COMMENT '人力管家职位ID',
    position_name VARCHAR(160) NOT NULL COMMENT '职位名称',
    raw_json JSON NOT NULL COMMENT '经过字段筛选后的第三方职位JSON数据',
    last_sync_task_id BIGINT NOT NULL COMMENT '最后同步任务ID',
    last_sync_time DATETIME NOT NULL COMMENT '最后同步时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标志：0未删除，1已删除',
    create_by BIGINT DEFAULT NULL COMMENT '创建人用户ID',
    update_by BIGINT DEFAULT NULL COMMENT '更新人用户ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_integration_rlgj_position (tenant_id, external_position_id),
    KEY idx_integration_rlgj_position_task (tenant_id, last_sync_task_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='人力管家职位数据镜像表';

CREATE TABLE IF NOT EXISTS integration_rlgj_post (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    tenant_id BIGINT NOT NULL COMMENT '租户ID',
    external_org_id VARCHAR(64) NOT NULL COMMENT '岗位所属的人力管家组织ID',
    external_post_id VARCHAR(64) NOT NULL COMMENT '人力管家岗位ID',
    external_parent_id VARCHAR(64) DEFAULT NULL COMMENT '人力管家上级岗位ID',
    external_position_id VARCHAR(64) DEFAULT NULL COMMENT '岗位关联的人力管家职位ID',
    post_name VARCHAR(160) NOT NULL COMMENT '岗位名称',
    post_type VARCHAR(64) DEFAULT NULL COMMENT '岗位分类ID',
    post_type_name VARCHAR(160) DEFAULT NULL COMMENT '岗位分类名称',
    raw_json JSON NOT NULL COMMENT '经过字段筛选后的第三方岗位JSON数据',
    last_sync_task_id BIGINT NOT NULL COMMENT '最后同步任务ID',
    last_sync_time DATETIME NOT NULL COMMENT '最后同步时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标志：0未删除，1已删除',
    create_by BIGINT DEFAULT NULL COMMENT '创建人用户ID',
    update_by BIGINT DEFAULT NULL COMMENT '更新人用户ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_integration_rlgj_post (tenant_id, external_org_id, external_post_id),
    KEY idx_integration_rlgj_post_position (tenant_id, external_position_id),
    KEY idx_integration_rlgj_post_parent (tenant_id, external_org_id, external_parent_id),
    KEY idx_integration_rlgj_post_task (tenant_id, last_sync_task_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='人力管家岗位数据镜像表';

CREATE TABLE IF NOT EXISTS integration_rlgj_user (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    tenant_id BIGINT NOT NULL COMMENT '租户ID',
    external_user_id VARCHAR(64) NOT NULL COMMENT '人力管家用户ID',
    external_org_id VARCHAR(64) DEFAULT NULL COMMENT '用户所属的人力管家组织ID',
    user_number VARCHAR(64) DEFAULT NULL COMMENT '员工工号',
    user_name VARCHAR(64) NOT NULL COMMENT '员工姓名',
    user_name_pinyin VARCHAR(160) DEFAULT NULL COMMENT '员工姓名拼音',
    gender_code VARCHAR(32) DEFAULT NULL COMMENT '性别编码：118001男，118002女',
    mobile VARCHAR(32) DEFAULT NULL COMMENT '手机号码',
    email VARCHAR(128) DEFAULT NULL COMMENT '电子邮箱',
    user_type VARCHAR(32) DEFAULT NULL COMMENT '人力管家用工类型编码',
    user_status VARCHAR(32) DEFAULT NULL COMMENT '人力管家员工状态编码',
    external_position_id VARCHAR(64) DEFAULT NULL COMMENT '人力管家职位ID',
    position_name VARCHAR(160) DEFAULT NULL COMMENT '职位名称',
    external_post_id VARCHAR(64) DEFAULT NULL COMMENT '人力管家岗位ID',
    post_name VARCHAR(160) DEFAULT NULL COMMENT '岗位名称',
    post_type_name VARCHAR(160) DEFAULT NULL COMMENT '岗位分类名称',
    external_rank_id VARCHAR(64) DEFAULT NULL COMMENT '人力管家职级ID',
    rank_name VARCHAR(160) DEFAULT NULL COMMENT '职级名称',
    leader_user_ids VARCHAR(500) DEFAULT NULL COMMENT '直接上级人力管家用户ID列表，多个ID使用逗号分隔',
    offer_date DATE DEFAULT NULL COMMENT '实际转正日期',
    start_date DATE DEFAULT NULL COMMENT '入职日期',
    raw_json JSON NOT NULL COMMENT '已移除证件号、银行卡号等敏感字段的第三方用户JSON数据',
    last_sync_task_id BIGINT NOT NULL COMMENT '最后同步任务ID',
    last_sync_time DATETIME NOT NULL COMMENT '最后同步时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标志：0未删除，1已删除',
    create_by BIGINT DEFAULT NULL COMMENT '创建人用户ID',
    update_by BIGINT DEFAULT NULL COMMENT '更新人用户ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_integration_rlgj_user (tenant_id, external_user_id),
    KEY idx_integration_rlgj_user_org (tenant_id, external_org_id),
    KEY idx_integration_rlgj_user_post (tenant_id, external_post_id),
    KEY idx_integration_rlgj_user_status (tenant_id, user_status, deleted),
    KEY idx_integration_rlgj_user_task (tenant_id, last_sync_task_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='人力管家用户数据镜像表';

CREATE TABLE IF NOT EXISTS sys_platform_organization (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '平台组织ID',
    parent_id BIGINT NOT NULL DEFAULT 0 COMMENT '上级平台组织ID，0表示根组织',
    org_code VARCHAR(64) NOT NULL COMMENT '平台组织编码',
    org_name VARCHAR(160) NOT NULL COMMENT '平台组织名称',
    org_type VARCHAR(32) NOT NULL DEFAULT 'DEPARTMENT' COMMENT '平台组织类型编码',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '平台组织状态：0停用，1启用',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '平台组织排序号',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标志：0未删除，1已删除',
    create_by BIGINT DEFAULT NULL COMMENT '创建人平台用户ID',
    update_by BIGINT DEFAULT NULL COMMENT '更新人平台用户ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_platform_org_code (org_code),
    KEY idx_sys_platform_org_parent (parent_id, status, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='平台端组织表';

CREATE TABLE IF NOT EXISTS sys_platform_user (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '平台用户ID',
    organization_id BIGINT DEFAULT NULL COMMENT '所属平台组织ID',
    username VARCHAR(64) NOT NULL COMMENT '平台登录账号',
    password_hash VARCHAR(255) NOT NULL COMMENT '平台登录密码哈希值',
    display_name VARCHAR(64) NOT NULL COMMENT '平台用户显示名称',
    email VARCHAR(128) DEFAULT NULL COMMENT '电子邮箱',
    phone VARCHAR(32) DEFAULT NULL COMMENT '手机号码',
    data_scope VARCHAR(32) NOT NULL DEFAULT 'SELF' COMMENT '数据权限范围编码',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '平台用户状态：0停用，1启用',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标志：0未删除，1已删除',
    create_by BIGINT DEFAULT NULL COMMENT '创建人平台用户ID',
    update_by BIGINT DEFAULT NULL COMMENT '更新人平台用户ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_platform_user_name (username),
    KEY idx_sys_platform_user_org (organization_id, status, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='平台端用户表';

CREATE TABLE IF NOT EXISTS sys_merchant_organization (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '商家组织ID',
    tenant_id BIGINT NOT NULL COMMENT '租户ID',
    parent_id BIGINT NOT NULL DEFAULT 0 COMMENT '上级商家组织ID，0表示根组织',
    org_code VARCHAR(64) NOT NULL COMMENT '商家组织编码',
    org_name VARCHAR(160) NOT NULL COMMENT '商家组织名称',
    org_type VARCHAR(32) NOT NULL COMMENT '商家组织类型编码',
    store_code VARCHAR(64) DEFAULT NULL COMMENT '门店编码，仅门店组织有值',
    source_type VARCHAR(32) NOT NULL DEFAULT 'LOCAL' COMMENT '数据来源：LOCAL本地、HR_BUTLER人力管家',
    source_id VARCHAR(64) DEFAULT NULL COMMENT '第三方来源记录ID，本地数据为空',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '商家组织状态：0停用，1启用',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '商家组织排序号',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标志：0未删除，1已删除',
    create_by BIGINT DEFAULT NULL COMMENT '创建人商家用户ID',
    update_by BIGINT DEFAULT NULL COMMENT '更新人商家用户ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_merchant_org_code (tenant_id, org_code),
    UNIQUE KEY uk_sys_merchant_org_source (tenant_id, source_type, source_id),
    KEY idx_sys_merchant_org_parent (tenant_id, parent_id, status, deleted),
    KEY idx_sys_merchant_org_store (tenant_id, store_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商家端组织表';

CREATE TABLE IF NOT EXISTS sys_merchant_user (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '商家用户ID',
    tenant_id BIGINT NOT NULL COMMENT '租户ID',
    organization_id BIGINT DEFAULT NULL COMMENT '所属商家组织ID',
    username VARCHAR(64) DEFAULT NULL COMMENT '商家登录账号，未开通登录时为空',
    password_hash VARCHAR(255) DEFAULT NULL COMMENT '商家登录密码哈希值，未开通登录时为空',
    employee_number VARCHAR(64) DEFAULT NULL COMMENT '员工工号',
    display_name VARCHAR(64) NOT NULL COMMENT '商家用户显示名称',
    name_pinyin VARCHAR(160) DEFAULT NULL COMMENT '姓名拼音',
    gender_code VARCHAR(32) DEFAULT NULL COMMENT '性别编码：118001男，118002女',
    email VARCHAR(128) DEFAULT NULL COMMENT '电子邮箱',
    phone VARCHAR(32) DEFAULT NULL COMMENT '手机号码',
    position_id VARCHAR(64) DEFAULT NULL COMMENT '第三方职位ID',
    position_name VARCHAR(160) DEFAULT NULL COMMENT '职位名称',
    post_id VARCHAR(64) DEFAULT NULL COMMENT '第三方岗位ID',
    post_name VARCHAR(160) DEFAULT NULL COMMENT '岗位名称',
    source_type VARCHAR(32) NOT NULL DEFAULT 'LOCAL' COMMENT '数据来源：LOCAL本地、HR_BUTLER人力管家',
    source_id VARCHAR(64) DEFAULT NULL COMMENT '第三方来源记录ID，本地数据为空',
    login_enabled TINYINT NOT NULL DEFAULT 0 COMMENT '是否允许登录：0不允许，1允许',
    data_scope VARCHAR(32) NOT NULL DEFAULT 'SELF' COMMENT '数据权限范围编码',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '商家用户状态：0停用，1启用',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标志：0未删除，1已删除',
    create_by BIGINT DEFAULT NULL COMMENT '创建人商家用户ID',
    update_by BIGINT DEFAULT NULL COMMENT '更新人商家用户ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_merchant_user_name (tenant_id, username),
    UNIQUE KEY uk_sys_merchant_user_source (tenant_id, source_type, source_id),
    KEY idx_sys_merchant_user_org (tenant_id, organization_id, status, deleted),
    KEY idx_sys_merchant_user_employee (tenant_id, employee_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商家端用户表';

INSERT INTO sys_platform_organization
    (id, parent_id, org_code, org_name, org_type, status, sort_order)
VALUES (1, 0, 'TQL_PLATFORM', '同庆楼SaaS平台', 'COMPANY', 1, 10)
ON DUPLICATE KEY UPDATE org_name = VALUES(org_name), status = VALUES(status), deleted = 0;

INSERT INTO sys_platform_user
    (id, organization_id, username, password_hash, display_name, email, phone, data_scope, status)
SELECT id, 1, username, password_hash, display_name, email, phone, data_scope, status
FROM sys_user
WHERE id = 1 AND client_type = 'PLATFORM'
ON DUPLICATE KEY UPDATE
    organization_id = VALUES(organization_id), password_hash = VALUES(password_hash),
    display_name = VALUES(display_name), email = VALUES(email), phone = VALUES(phone),
    data_scope = VALUES(data_scope), status = VALUES(status), deleted = 0;

INSERT INTO sys_merchant_organization
    (id, tenant_id, parent_id, org_code, org_name, org_type, store_code, source_type, source_id, status, sort_order)
SELECT id, tenant_id, parent_id, store_code, store_name, 'STORE', store_code, 'LOCAL', NULL, status, sort_order
FROM sys_store
ON DUPLICATE KEY UPDATE
    parent_id = VALUES(parent_id), org_name = VALUES(org_name), store_code = VALUES(store_code),
    status = VALUES(status), sort_order = VALUES(sort_order), deleted = 0;

INSERT INTO sys_merchant_user
    (id, tenant_id, organization_id, username, password_hash, display_name, email, phone,
     source_type, source_id, login_enabled, data_scope, status)
SELECT id, tenant_id, primary_store_id, username, password_hash, display_name, email, phone,
       'LOCAL', NULL, 1, data_scope, status
FROM sys_user
WHERE client_type = 'MERCHANT'
ON DUPLICATE KEY UPDATE
    organization_id = VALUES(organization_id), password_hash = VALUES(password_hash),
    display_name = VALUES(display_name), email = VALUES(email), phone = VALUES(phone),
    login_enabled = VALUES(login_enabled), data_scope = VALUES(data_scope),
    status = VALUES(status), deleted = 0;
