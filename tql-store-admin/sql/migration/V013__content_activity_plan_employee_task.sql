USE tql_store_saas;

CREATE TABLE IF NOT EXISTS ops_content_activity (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '营销活动ID',
    tenant_id BIGINT NOT NULL COMMENT '租户ID',
    activity_name VARCHAR(100) NOT NULL COMMENT '营销活动名称',
    objective VARCHAR(500) DEFAULT NULL COMMENT '营销活动目标说明',
    start_time DATETIME(3) NOT NULL COMMENT '活动开始时间',
    end_time DATETIME(3) NOT NULL COMMENT '活动结束时间',
    owner_id BIGINT NOT NULL COMMENT '活动负责人商家用户ID',
    status VARCHAR(32) NOT NULL COMMENT '活动状态编码：DRAFT草稿、ACTIVE进行中、TERMINATED已终止',
    version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    create_by BIGINT NOT NULL COMMENT '创建人商家用户ID',
    create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    update_by BIGINT NOT NULL COMMENT '更新人商家用户ID',
    update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标志：0未删除、1已删除',
    PRIMARY KEY (id),
    KEY idx_content_activity_query (tenant_id, status, create_time),
    KEY idx_content_activity_owner (tenant_id, owner_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI内容营销活动表';

CREATE TABLE IF NOT EXISTS ops_content_plan (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '内容计划ID',
    tenant_id BIGINT NOT NULL COMMENT '租户ID',
    activity_id BIGINT NOT NULL COMMENT '所属营销活动ID',
    plan_name VARCHAR(100) NOT NULL COMMENT '内容计划名称',
    task_instruction VARCHAR(1000) NOT NULL COMMENT '员工任务说明',
    creation_mode VARCHAR(32) NOT NULL COMMENT '创作模式编码',
    storyboard_count INT NOT NULL DEFAULT 3 COMMENT '分镜数量快照，原创任务为1',
    training_policy VARCHAR(32) NOT NULL COMMENT '前置训练策略编码',
    deadline DATETIME(3) NOT NULL COMMENT '员工任务截止时间',
    status VARCHAR(32) NOT NULL COMMENT '内容计划状态编码：DRAFT草稿、ACTIVE已发布、ENDED已结束、TERMINATED已终止',
    current_version_no INT NOT NULL DEFAULT 0 COMMENT '当前已发布计划版本号',
    version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    create_by BIGINT NOT NULL COMMENT '创建人商家用户ID',
    create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    update_by BIGINT NOT NULL COMMENT '更新人商家用户ID',
    update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标志：0未删除、1已删除',
    PRIMARY KEY (id),
    KEY idx_content_plan_activity (tenant_id, activity_id, status),
    KEY idx_content_plan_query (tenant_id, status, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI内容计划表';

CREATE TABLE IF NOT EXISTS ops_content_plan_version (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '内容计划版本ID',
    tenant_id BIGINT NOT NULL COMMENT '租户ID',
    plan_id BIGINT NOT NULL COMMENT '内容计划ID',
    version_no INT NOT NULL COMMENT '计划版本号',
    activity_name VARCHAR(100) NOT NULL COMMENT '发布时固化的营销活动名称',
    plan_name VARCHAR(100) NOT NULL COMMENT '发布时固化的内容计划名称',
    task_instruction VARCHAR(1000) NOT NULL COMMENT '发布时固化的员工任务说明',
    creation_mode VARCHAR(32) NOT NULL COMMENT '发布时固化的创作模式编码',
    storyboard_count INT NOT NULL DEFAULT 3 COMMENT '发布时固化的分镜数量',
    training_policy VARCHAR(32) NOT NULL COMMENT '发布时固化的前置训练策略编码',
    deadline DATETIME(3) NOT NULL COMMENT '发布时固化的任务截止时间',
    published_by BIGINT NOT NULL COMMENT '计划发布人商家用户ID',
    published_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '计划发布时间',
    create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_content_plan_version (tenant_id, plan_id, version_no),
    KEY idx_content_plan_version_plan (tenant_id, plan_id, published_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI内容计划发布版本表';

CREATE TABLE IF NOT EXISTS ops_content_publish_request (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '计划发布幂等请求ID',
    tenant_id BIGINT NOT NULL COMMENT '租户ID',
    plan_id BIGINT NOT NULL COMMENT '内容计划ID',
    idempotency_key VARCHAR(128) NOT NULL COMMENT '客户端发布幂等键',
    plan_version_id BIGINT DEFAULT NULL COMMENT '生成的计划版本ID',
    request_fingerprint VARCHAR(64) NOT NULL COMMENT '发布请求内容SHA-256摘要',
    status VARCHAR(16) NOT NULL COMMENT '发布请求状态编码：PROCESSING处理中、SUCCESS成功、FAILED失败',
    created_count INT NOT NULL DEFAULT 0 COMMENT '成功创建员工任务数量',
    failed_count INT NOT NULL DEFAULT 0 COMMENT '未创建员工任务数量',
    error_message VARCHAR(500) DEFAULT NULL COMMENT '发布请求失败原因',
    create_by BIGINT NOT NULL COMMENT '发布请求人商家用户ID',
    create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_content_publish_idempotency (tenant_id, idempotency_key),
    KEY idx_content_publish_plan (tenant_id, plan_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI内容计划发布幂等请求表';

CREATE TABLE IF NOT EXISTS ops_content_employee_task (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '员工内容任务ID',
    tenant_id BIGINT NOT NULL COMMENT '租户ID',
    plan_id BIGINT NOT NULL COMMENT '内容计划ID',
    plan_version_id BIGINT NOT NULL COMMENT '内容计划版本ID',
    employee_id BIGINT NOT NULL COMMENT '任务接收员工ID',
    store_id BIGINT DEFAULT NULL COMMENT '员工主要所属门店ID快照',
    activity_name VARCHAR(100) NOT NULL COMMENT '营销活动名称快照',
    plan_name VARCHAR(100) NOT NULL COMMENT '内容计划名称快照',
    task_instruction VARCHAR(1000) NOT NULL COMMENT '员工任务说明快照',
    creation_mode VARCHAR(32) NOT NULL DEFAULT 'AI_ASSISTED' COMMENT '创作模式快照',
    storyboard_count INT NOT NULL DEFAULT 3 COMMENT '分镜数量快照，原创任务为1',
    current_stage VARCHAR(32) NOT NULL COMMENT '员工内容任务当前阶段编码',
    deadline DATETIME(3) NOT NULL COMMENT '员工任务截止时间',
    completion_time DATETIME(3) DEFAULT NULL COMMENT '任务首次满足完成条件时间',
    version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    create_by BIGINT NOT NULL COMMENT '任务下发人商家用户ID',
    create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    update_by BIGINT NOT NULL COMMENT '任务更新人商家用户ID',
    update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标志：0未删除、1已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_content_task_employee (tenant_id, plan_version_id, employee_id),
    KEY idx_content_task_employee_query (tenant_id, employee_id, current_stage, deadline),
    KEY idx_content_task_plan (tenant_id, plan_id, current_stage)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI内容员工执行任务表';

UPDATE sys_menu
SET menu_name = 'AI内容中心',
    route_name = 'MerchantAiContent',
    route_path = '/content',
    component_key = 'ai-content',
    icon = 'IconVideoCamera',
    update_time = CURRENT_TIMESTAMP
WHERE client_type = 'MERCHANT'
  AND permission_code = 'merchant:content:view'
  AND deleted = 0;

INSERT INTO sys_menu
    (tenant_id, parent_id, menu_name, menu_type, route_name, route_path, component_key,
     icon, permission_code, client_type, sort_order, visible, status, system_builtin, deleted)
SELECT tenant_id, id, '创建内容计划', 'BUTTON', NULL, NULL, NULL,
       NULL, 'merchant:content:plan:create', 'MERCHANT', 10, 0, 1, 1, 0
FROM sys_menu parent
WHERE parent.client_type = 'MERCHANT'
  AND parent.permission_code = 'merchant:content:view'
  AND parent.deleted = 0
  AND NOT EXISTS (
      SELECT 1 FROM sys_menu existing
      WHERE existing.tenant_id = parent.tenant_id
        AND existing.client_type = 'MERCHANT'
        AND existing.permission_code = 'merchant:content:plan:create'
        AND existing.deleted = 0
  );

INSERT INTO sys_menu
    (tenant_id, parent_id, menu_name, menu_type, route_name, route_path, component_key,
     icon, permission_code, client_type, sort_order, visible, status, system_builtin, deleted)
SELECT tenant_id, id, '发布内容计划', 'BUTTON', NULL, NULL, NULL,
       NULL, 'merchant:content:plan:publish', 'MERCHANT', 20, 0, 1, 1, 0
FROM sys_menu parent
WHERE parent.client_type = 'MERCHANT'
  AND parent.permission_code = 'merchant:content:view'
  AND parent.deleted = 0
  AND NOT EXISTS (
      SELECT 1 FROM sys_menu existing
      WHERE existing.tenant_id = parent.tenant_id
        AND existing.client_type = 'MERCHANT'
        AND existing.permission_code = 'merchant:content:plan:publish'
        AND existing.deleted = 0
  );

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT role.id, menu.id
FROM sys_role role
JOIN sys_menu menu
  ON menu.tenant_id = role.tenant_id
 AND menu.client_type = 'MERCHANT'
 AND menu.permission_code IN ('merchant:content:plan:create', 'merchant:content:plan:publish')
 AND menu.deleted = 0
WHERE role.client_type = 'MERCHANT'
  AND role.role_code = 'MERCHANT_ADMIN'
  AND role.status = 1;
