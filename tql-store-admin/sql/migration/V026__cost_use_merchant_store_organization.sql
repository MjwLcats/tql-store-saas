USE tql_store_saas;

-- 成本模块统一使用商家组织中的门店节点。该表也是系统管理门店下拉框的数据来源。
-- 只调整当前 SaaS 本地库的外键目标，不迁移、不删除任何业务数据。

ALTER TABLE cost_material_price
    DROP FOREIGN KEY fk_cost_material_price_store;
ALTER TABLE cost_material_price
    ADD CONSTRAINT fk_cost_material_price_store
        FOREIGN KEY (store_id) REFERENCES sys_merchant_organization (id);

ALTER TABLE cost_bom
    DROP FOREIGN KEY fk_cost_bom_store;
ALTER TABLE cost_bom
    ADD CONSTRAINT fk_cost_bom_store
        FOREIGN KEY (store_id) REFERENCES sys_merchant_organization (id);

ALTER TABLE cost_inventory_task
    DROP FOREIGN KEY fk_cost_inventory_task_store;
ALTER TABLE cost_inventory_task
    ADD CONSTRAINT fk_cost_inventory_task_store
        FOREIGN KEY (store_id) REFERENCES sys_merchant_organization (id);
