USE tql_store_saas;

-- 仅创建 SaaS 原生 BOM 与盘点基础结构。本文件不读取、更新或删除旧成本系统数据。
CREATE TABLE IF NOT EXISTS cost_material_unit (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '单位主键',
    tenant_id BIGINT NOT NULL COMMENT '租户主键',
    unit_code VARCHAR(64) NOT NULL COMMENT '单位编码',
    unit_name VARCHAR(64) NOT NULL COMMENT '单位名称',
    decimal_scale INT NOT NULL DEFAULT 6 COMMENT '数量小数位数',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1启用，0停用',
    created_by BIGINT NOT NULL COMMENT '创建人主键',
    created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_by BIGINT NOT NULL COMMENT '更新人主键',
    updated_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记：0正常，1删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_cost_material_unit_code (tenant_id, unit_code, deleted),
    KEY idx_cost_material_unit_tenant (tenant_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='成本物料计量单位';

CREATE TABLE IF NOT EXISTS cost_material (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '物料主键',
    tenant_id BIGINT NOT NULL COMMENT '租户主键',
    material_code VARCHAR(64) NOT NULL COMMENT 'SaaS物料编码',
    material_name VARCHAR(200) NOT NULL COMMENT '物料名称',
    specification VARCHAR(200) NULL COMMENT '规格型号',
    base_unit_id BIGINT NOT NULL COMMENT '基本单位主键',
    external_material_code VARCHAR(128) NULL COMMENT '外部系统物料编码',
    source_system VARCHAR(32) NOT NULL DEFAULT 'SAAS' COMMENT '来源系统',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1启用，0停用',
    created_by BIGINT NOT NULL COMMENT '创建人主键',
    created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_by BIGINT NOT NULL COMMENT '更新人主键',
    updated_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记：0正常，1删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_cost_material_code (tenant_id, material_code, deleted),
    UNIQUE KEY uk_cost_material_external (tenant_id, source_system, external_material_code, deleted),
    KEY idx_cost_material_tenant_name (tenant_id, material_name),
    CONSTRAINT fk_cost_material_base_unit FOREIGN KEY (base_unit_id) REFERENCES cost_material_unit (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='成本物料主数据';

CREATE TABLE IF NOT EXISTS cost_material_unit_conversion (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '换算关系主键',
    tenant_id BIGINT NOT NULL COMMENT '租户主键',
    material_id BIGINT NOT NULL COMMENT '物料主键',
    source_unit_id BIGINT NOT NULL COMMENT '来源单位主键',
    target_unit_id BIGINT NOT NULL COMMENT '目标单位主键',
    conversion_rate DECIMAL(24,10) NOT NULL COMMENT '换算系数：目标数量等于来源数量乘以换算系数',
    created_by BIGINT NOT NULL COMMENT '创建人主键',
    created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_by BIGINT NOT NULL COMMENT '更新人主键',
    updated_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记：0正常，1删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_cost_material_conversion (tenant_id, material_id, source_unit_id, target_unit_id, deleted),
    CONSTRAINT fk_cost_conversion_material FOREIGN KEY (material_id) REFERENCES cost_material (id),
    CONSTRAINT fk_cost_conversion_source_unit FOREIGN KEY (source_unit_id) REFERENCES cost_material_unit (id),
    CONSTRAINT fk_cost_conversion_target_unit FOREIGN KEY (target_unit_id) REFERENCES cost_material_unit (id),
    CONSTRAINT ck_cost_conversion_rate CHECK (conversion_rate > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='物料单位换算关系';

CREATE TABLE IF NOT EXISTS cost_dish (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '菜品主键',
    tenant_id BIGINT NOT NULL COMMENT '租户主键',
    dish_code VARCHAR(64) NOT NULL COMMENT 'SaaS菜品编码',
    dish_name VARCHAR(200) NOT NULL COMMENT '菜品名称',
    external_dish_code VARCHAR(128) NULL COMMENT '外部系统菜品编码',
    source_system VARCHAR(32) NOT NULL DEFAULT 'SAAS' COMMENT '来源系统',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1启用，0停用',
    created_by BIGINT NOT NULL COMMENT '创建人主键',
    created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_by BIGINT NOT NULL COMMENT '更新人主键',
    updated_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记：0正常，1删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_cost_dish_code (tenant_id, dish_code, deleted),
    UNIQUE KEY uk_cost_dish_external (tenant_id, source_system, external_dish_code, deleted),
    KEY idx_cost_dish_tenant_name (tenant_id, dish_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='成本菜品主数据';

CREATE TABLE IF NOT EXISTS cost_material_price (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '物料价格主键',
    tenant_id BIGINT NOT NULL COMMENT '租户主键',
    store_id BIGINT NOT NULL COMMENT '门店主键',
    material_id BIGINT NOT NULL COMMENT '物料主键',
    price_type VARCHAR(32) NOT NULL COMMENT '价格类型：STANDARD标准价，PURCHASE采购价，MANUAL手工价',
    unit_price DECIMAL(18,6) NOT NULL COMMENT '基本单位未税单价',
    effective_from DATETIME NOT NULL COMMENT '生效开始时间',
    effective_to DATETIME NULL COMMENT '生效结束时间',
    source_system VARCHAR(32) NOT NULL DEFAULT 'SAAS' COMMENT '来源系统',
    created_by BIGINT NOT NULL COMMENT '创建人主键',
    created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_by BIGINT NOT NULL COMMENT '更新人主键',
    updated_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记：0正常，1删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_cost_material_price_period (tenant_id, store_id, material_id, price_type, effective_from, deleted),
    KEY idx_cost_material_price_effective (tenant_id, store_id, material_id, effective_from, effective_to),
    CONSTRAINT fk_cost_material_price_store FOREIGN KEY (store_id) REFERENCES sys_store (id),
    CONSTRAINT fk_cost_material_price_material FOREIGN KEY (material_id) REFERENCES cost_material (id),
    CONSTRAINT ck_cost_material_price_value CHECK (unit_price >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='门店物料价格快照';

CREATE TABLE IF NOT EXISTS cost_bom (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'BOM主键',
    tenant_id BIGINT NOT NULL COMMENT '租户主键',
    store_id BIGINT NOT NULL COMMENT '门店主键',
    dish_id BIGINT NOT NULL COMMENT '菜品主键',
    status VARCHAR(32) NOT NULL COMMENT '状态：DRAFT待编辑，PENDING待审核，PUBLISHED已发布，REJECTED已驳回，DISABLED已停用',
    current_version INT NOT NULL DEFAULT 1 COMMENT '当前版本号',
    remark VARCHAR(500) NULL COMMENT '备注',
    created_by BIGINT NOT NULL COMMENT '创建人主键',
    created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_by BIGINT NOT NULL COMMENT '更新人主键',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记：0正常，1删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_cost_bom_store_dish (tenant_id, store_id, dish_id, deleted),
    KEY idx_cost_bom_tenant_status (tenant_id, store_id, status),
    CONSTRAINT fk_cost_bom_store FOREIGN KEY (store_id) REFERENCES sys_store (id),
    CONSTRAINT fk_cost_bom_dish FOREIGN KEY (dish_id) REFERENCES cost_dish (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='门店菜品BOM';

CREATE TABLE IF NOT EXISTS cost_bom_version (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'BOM版本主键',
    tenant_id BIGINT NOT NULL COMMENT '租户主键',
    bom_id BIGINT NOT NULL COMMENT 'BOM主键',
    version_no INT NOT NULL COMMENT '版本号',
    status VARCHAR(32) NOT NULL COMMENT '版本状态',
    total_cost DECIMAL(18,4) NULL COMMENT '发布时总成本',
    effective_from DATETIME NULL COMMENT '生效开始时间',
    effective_to DATETIME NULL COMMENT '生效结束时间',
    published_by BIGINT NULL COMMENT '发布人主键',
    published_time DATETIME NULL COMMENT '发布时间',
    remark VARCHAR(500) NULL COMMENT '备注',
    created_by BIGINT NOT NULL COMMENT '创建人主键',
    created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_cost_bom_version (tenant_id, bom_id, version_no),
    KEY idx_cost_bom_version_effective (tenant_id, bom_id, status, effective_from, effective_to),
    CONSTRAINT fk_cost_bom_version_bom FOREIGN KEY (bom_id) REFERENCES cost_bom (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='BOM不可变版本';

CREATE TABLE IF NOT EXISTS cost_bom_item (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'BOM明细主键',
    tenant_id BIGINT NOT NULL COMMENT '租户主键',
    bom_id BIGINT NOT NULL COMMENT 'BOM主键',
    version_no INT NOT NULL COMMENT 'BOM版本号',
    material_id BIGINT NOT NULL COMMENT '物料主键',
    unit_id BIGINT NOT NULL COMMENT '用量单位主键',
    quantity DECIMAL(18,6) NOT NULL COMMENT '物料用量',
    converted_quantity DECIMAL(18,6) NULL COMMENT '换算为基本单位后的用量',
    unit_price DECIMAL(18,6) NULL COMMENT '成本计算时单位价格',
    item_cost DECIMAL(18,4) NULL COMMENT '明细成本',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序号',
    created_by BIGINT NOT NULL COMMENT '创建人主键',
    created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_cost_bom_item_material (tenant_id, bom_id, version_no, material_id),
    KEY idx_cost_bom_item_bom (tenant_id, bom_id, version_no),
    CONSTRAINT fk_cost_bom_item_bom FOREIGN KEY (bom_id) REFERENCES cost_bom (id),
    CONSTRAINT fk_cost_bom_item_material FOREIGN KEY (material_id) REFERENCES cost_material (id),
    CONSTRAINT fk_cost_bom_item_unit FOREIGN KEY (unit_id) REFERENCES cost_material_unit (id),
    CONSTRAINT ck_cost_bom_item_quantity CHECK (quantity > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='BOM版本物料明细';

CREATE TABLE IF NOT EXISTS cost_inventory_task (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '盘点任务主键',
    tenant_id BIGINT NOT NULL COMMENT '租户主键',
    store_id BIGINT NOT NULL COMMENT '门店主键',
    task_code VARCHAR(64) NOT NULL COMMENT '盘点任务编码',
    task_name VARCHAR(200) NOT NULL COMMENT '盘点任务名称',
    status VARCHAR(32) NOT NULL COMMENT '状态：DRAFT草稿，IN_PROGRESS盘点中，PENDING_REVIEW待审核，REJECTED已驳回，APPROVED已审核，CLOSED已关账',
    planned_start_time DATETIME NOT NULL COMMENT '计划开始时间',
    planned_end_time DATETIME NOT NULL COMMENT '计划结束时间',
    submitted_time DATETIME NULL COMMENT '提交时间',
    approved_time DATETIME NULL COMMENT '审核时间',
    approved_by BIGINT NULL COMMENT '审核人主键',
    closed_time DATETIME NULL COMMENT '关账时间',
    remark VARCHAR(500) NULL COMMENT '备注',
    created_by BIGINT NOT NULL COMMENT '创建人主键',
    created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_by BIGINT NOT NULL COMMENT '更新人主键',
    updated_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记：0正常，1删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_cost_inventory_task_code (tenant_id, task_code),
    KEY idx_cost_inventory_task_store_status (tenant_id, store_id, status),
    KEY idx_cost_inventory_task_time (tenant_id, planned_start_time, planned_end_time),
    CONSTRAINT fk_cost_inventory_task_store FOREIGN KEY (store_id) REFERENCES sys_store (id),
    CONSTRAINT ck_cost_inventory_task_time CHECK (planned_end_time > planned_start_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='门店盘点任务';

CREATE TABLE IF NOT EXISTS cost_inventory_snapshot (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '盘点快照主键',
    tenant_id BIGINT NOT NULL COMMENT '租户主键',
    store_id BIGINT NOT NULL COMMENT '门店主键',
    task_id BIGINT NOT NULL COMMENT '盘点任务主键',
    location_code VARCHAR(64) NOT NULL COMMENT '盘点库位编码快照',
    location_name VARCHAR(200) NOT NULL COMMENT '盘点库位名称快照',
    material_id BIGINT NOT NULL COMMENT '物料主键',
    material_code VARCHAR(64) NOT NULL COMMENT '物料编码快照',
    material_name VARCHAR(200) NOT NULL COMMENT '物料名称快照',
    specification VARCHAR(200) NULL COMMENT '规格快照',
    count_unit_id BIGINT NOT NULL COMMENT '盘点单位主键',
    count_unit_name VARCHAR(64) NOT NULL COMMENT '盘点单位名称快照',
    conversion_rate DECIMAL(24,10) NOT NULL COMMENT '盘点单位转基本单位系数快照',
    book_quantity DECIMAL(18,6) NULL COMMENT '账面数量快照',
    unit_price DECIMAL(18,6) NULL COMMENT '盘点价格快照',
    created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '快照创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_cost_inventory_snapshot (tenant_id, task_id, location_code, material_id),
    KEY idx_cost_inventory_snapshot_task (tenant_id, store_id, task_id),
    CONSTRAINT fk_cost_inventory_snapshot_task FOREIGN KEY (task_id) REFERENCES cost_inventory_task (id),
    CONSTRAINT fk_cost_inventory_snapshot_material FOREIGN KEY (material_id) REFERENCES cost_material (id),
    CONSTRAINT fk_cost_inventory_snapshot_unit FOREIGN KEY (count_unit_id) REFERENCES cost_material_unit (id),
    CONSTRAINT ck_cost_inventory_snapshot_rate CHECK (conversion_rate > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='盘点任务物料不可变快照';

CREATE TABLE IF NOT EXISTS cost_inventory_count (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '实盘记录主键',
    tenant_id BIGINT NOT NULL COMMENT '租户主键',
    store_id BIGINT NOT NULL COMMENT '门店主键',
    task_id BIGINT NOT NULL COMMENT '盘点任务主键',
    snapshot_id BIGINT NOT NULL COMMENT '盘点快照主键',
    counter_id BIGINT NOT NULL COMMENT '盘点人主键',
    count_round INT NOT NULL DEFAULT 1 COMMENT '盘点轮次：1初盘，2复盘',
    counted_quantity DECIMAL(18,6) NOT NULL COMMENT '按盘点单位录入的实盘数量',
    base_quantity DECIMAL(18,6) NOT NULL COMMENT '换算后的基本单位数量',
    status VARCHAR(32) NOT NULL DEFAULT 'SAVED' COMMENT '状态：SAVED暂存，SUBMITTED已提交，REJECTED已驳回，APPROVED已审核',
    idempotency_key VARCHAR(128) NOT NULL COMMENT '客户端幂等键',
    submitted_time DATETIME NULL COMMENT '提交时间',
    created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    PRIMARY KEY (id),
    UNIQUE KEY uk_cost_inventory_count_idempotency (tenant_id, idempotency_key),
    UNIQUE KEY uk_cost_inventory_count_round (tenant_id, task_id, snapshot_id, counter_id, count_round),
    KEY idx_cost_inventory_count_task (tenant_id, store_id, task_id, status),
    CONSTRAINT fk_cost_inventory_count_task FOREIGN KEY (task_id) REFERENCES cost_inventory_task (id),
    CONSTRAINT fk_cost_inventory_count_snapshot FOREIGN KEY (snapshot_id) REFERENCES cost_inventory_snapshot (id),
    CONSTRAINT ck_cost_inventory_count_quantity CHECK (counted_quantity >= 0 AND base_quantity >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='盘点实盘记录';

-- 为已有商户创建 BOM 与盘点菜单。权限记录只写入当前 SaaS 数据库迁移脚本，不在此处执行。
INSERT INTO sys_menu
    (tenant_id, parent_id, menu_name, menu_type, route_name, route_path, component_key,
     icon, permission_code, client_type, sort_order, visible, status, system_builtin, deleted)
SELECT tenant.id, 0, '成本库存', 'DIRECTORY', NULL, '/cost', NULL,
       'icon-storage', NULL, 'MERCHANT', 60, 1, 1, 0, 0
FROM sys_tenant tenant
WHERE tenant.status = 1
  AND NOT EXISTS (
      SELECT 1 FROM sys_menu menu
      WHERE menu.tenant_id = tenant.id
        AND menu.client_type = 'MERCHANT'
        AND menu.menu_name = '成本库存'
        AND menu.menu_type = 'DIRECTORY'
        AND menu.deleted = 0
  );

INSERT INTO sys_menu
    (tenant_id, parent_id, menu_name, menu_type, route_name, route_path, component_key,
     icon, permission_code, client_type, sort_order, visible, status, system_builtin, deleted)
SELECT parent.tenant_id, parent.id, page.menu_name, 'MENU', page.route_name, page.route_path,
       page.component_key, page.icon, NULL, 'MERCHANT', page.sort_order, 1, 1, 0, 0
FROM sys_menu parent
JOIN (
    SELECT 'BOM管理' menu_name, 'MerchantCostBom' route_name, '/cost/boms' route_path,
           'MerchantCostBom' component_key, 'icon-list' icon, 10 sort_order
    UNION ALL
    SELECT '盘点管理', 'MerchantInventoryTasks', '/cost/inventory-tasks',
           'MerchantInventoryTasks', 'icon-storage', 20
    UNION ALL
    SELECT '成本主数据', 'MerchantCostMasterData', '/cost/master-data',
           'MerchantCostMasterData', 'icon-apps', 30
) page
WHERE parent.client_type = 'MERCHANT'
  AND parent.menu_name = '成本库存'
  AND parent.menu_type = 'DIRECTORY'
  AND parent.deleted = 0
  AND NOT EXISTS (
      SELECT 1 FROM sys_menu existing
      WHERE existing.tenant_id = parent.tenant_id
        AND existing.client_type = 'MERCHANT'
        AND existing.route_name = page.route_name
        AND existing.deleted = 0
  );

INSERT INTO sys_menu
    (tenant_id, parent_id, menu_name, menu_type, route_name, route_path, component_key,
     icon, permission_code, client_type, sort_order, visible, status, system_builtin, deleted)
SELECT page.tenant_id, page.id, permission_item.menu_name, 'BUTTON', NULL, NULL, NULL,
       NULL, permission_item.permission_code, 'MERCHANT', permission_item.sort_order,
       0, 1, 0, 0
FROM sys_menu page
JOIN (
    SELECT 'MerchantCostBom' page_route, '查询BOM' menu_name, 'merchant:cost:bom:query' permission_code, 10 sort_order
    UNION ALL SELECT 'MerchantCostBom', '新建BOM', 'merchant:cost:bom:create', 20
    UNION ALL SELECT 'MerchantCostBom', '编辑BOM', 'merchant:cost:bom:update', 30
    UNION ALL SELECT 'MerchantCostBom', '提交BOM审核', 'merchant:cost:bom:submit', 40
    UNION ALL SELECT 'MerchantCostBom', '审核与发布BOM', 'merchant:cost:bom:review', 50
    UNION ALL SELECT 'MerchantInventoryTasks', '查询盘点任务', 'merchant:cost:inventory:query', 10
    UNION ALL SELECT 'MerchantInventoryTasks', '新建盘点任务', 'merchant:cost:inventory:create', 20
    UNION ALL SELECT 'MerchantInventoryTasks', '执行盘点', 'merchant:cost:inventory:count', 30
    UNION ALL SELECT 'MerchantInventoryTasks', '审核盘点', 'merchant:cost:inventory:review', 40
    UNION ALL SELECT 'MerchantInventoryTasks', '盘点关账', 'merchant:cost:inventory:close', 50
    UNION ALL SELECT 'MerchantCostMasterData', '查询成本主数据', 'merchant:cost:master:query', 10
    UNION ALL SELECT 'MerchantCostMasterData', '维护成本主数据', 'merchant:cost:master:manage', 20
    UNION ALL SELECT 'MerchantCostMasterData', '维护物料价格', 'merchant:cost:price:manage', 30
) permission_item ON permission_item.page_route = page.route_name
WHERE page.client_type = 'MERCHANT'
  AND page.menu_type = 'MENU'
  AND page.deleted = 0
  AND NOT EXISTS (
      SELECT 1 FROM sys_menu existing
      WHERE existing.tenant_id = page.tenant_id
        AND existing.client_type = 'MERCHANT'
        AND existing.permission_code = permission_item.permission_code
        AND existing.deleted = 0
  );

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT role.id, menu.id
FROM sys_role role
JOIN sys_menu menu
  ON menu.tenant_id = role.tenant_id
 AND menu.client_type = 'MERCHANT'
 AND (menu.route_path LIKE '/cost%' OR menu.permission_code LIKE 'merchant:cost:%' OR menu.menu_name = '成本库存')
 AND menu.deleted = 0
 AND menu.status = 1
WHERE role.client_type = 'MERCHANT'
  AND UPPER(role.role_code) IN ('MERCHANT_ADMIN', 'ADMIN', 'SUPER_ADMIN')
  AND role.status = 1;
