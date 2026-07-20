USE tql_store_saas;
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS integration_sync_task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '同步任务ID',
    tenant_id BIGINT NOT NULL COMMENT '租户ID',
    provider VARCHAR(32) NOT NULL COMMENT '第三方数据来源编码',
    data_type VARCHAR(32) NOT NULL COMMENT '同步数据类型编码',
    sync_mode VARCHAR(16) NOT NULL COMMENT '同步方式：INCREMENTAL增量，FULL全量',
    trigger_type VARCHAR(16) NOT NULL COMMENT '触发方式：MANUAL手动，SCHEDULED定时，RETRY重试',
    retry_of BIGINT COMMENT '原失败任务ID',
    range_start DATE COMMENT '同步业务日期起始值',
    range_end DATE COMMENT '同步业务日期结束值',
    status VARCHAR(16) NOT NULL COMMENT '任务状态：PENDING待执行，RUNNING执行中，SUCCESS成功，FAILED失败',
    total_count INT NOT NULL DEFAULT 0 COMMENT '待处理数据总数',
    success_count INT NOT NULL DEFAULT 0 COMMENT '处理成功数量',
    failed_count INT NOT NULL DEFAULT 0 COMMENT '处理失败数量',
    error_message VARCHAR(1000) COMMENT '任务失败原因',
    active_lock_key VARCHAR(160) COMMENT '运行中任务防重复锁键',
    created_by BIGINT NOT NULL COMMENT '任务创建人用户ID',
    started_at DATETIME COMMENT '任务开始执行时间',
    finished_at DATETIME COMMENT '任务执行结束时间',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '任务创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '任务更新时间',
    UNIQUE KEY uk_integration_sync_active (active_lock_key),
    KEY idx_integration_sync_tenant_time (tenant_id, create_time),
    KEY idx_integration_sync_query (tenant_id, provider, data_type, status),
    KEY idx_integration_sync_retry (retry_of)
) ENGINE=InnoDB COMMENT='第三方数据同步任务表';

CREATE TABLE IF NOT EXISTS integration_sync_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '同步日志ID',
    tenant_id BIGINT NOT NULL COMMENT '租户ID',
    task_id BIGINT NOT NULL COMMENT '同步任务ID',
    log_level VARCHAR(16) NOT NULL COMMENT '日志级别',
    stage VARCHAR(32) NOT NULL COMMENT '任务执行阶段',
    message VARCHAR(500) NOT NULL COMMENT '日志消息',
    detail TEXT COMMENT '日志详细信息',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '日志创建时间',
    KEY idx_integration_log_task (tenant_id, task_id, id)
) ENGINE=InnoDB COMMENT='第三方数据同步日志表';

INSERT INTO sys_menu
    (id, tenant_id, parent_id, menu_name, route_name, route_path, component_key,
     icon, permission_code, client_type, sort_order, visible)
VALUES
    (205, 10001, 0, '第三方数据同步', 'MerchantIntegrationSync', '/integration/sync',
     'integration-sync', 'IconSync', 'merchant:integration:sync:view', 'MERCHANT', 50, 1)
ON DUPLICATE KEY UPDATE
    menu_name = VALUES(menu_name),
    route_path = VALUES(route_path),
    component_key = VALUES(component_key),
    icon = VALUES(icon),
    permission_code = VALUES(permission_code),
    sort_order = VALUES(sort_order),
    visible = VALUES(visible);

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT id, 205 FROM sys_role
WHERE tenant_id = 10001 AND client_type = 'MERCHANT' AND role_code = 'MERCHANT_ADMIN';
