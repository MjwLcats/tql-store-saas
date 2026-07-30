-- LOCAL DEVELOPMENT ONLY. Never execute against a shared or production database.
USE tql_store_saas;
SET @tenant_id = 10001;
SET @user_id = 2;
SET @store_id = 11002;

UPDATE sys_store SET status=1
WHERE tenant_id=@tenant_id AND id IN (11001,11002,11003,11004);
UPDATE sys_merchant_user SET primary_store_id=@store_id
WHERE id=@user_id AND tenant_id=@tenant_id AND primary_store_id IS NULL;

INSERT INTO cost_material_unit
 (tenant_id,unit_code,unit_name,decimal_scale,status,created_by,updated_by)
SELECT @tenant_id,'KG','千克',3,1,@user_id,@user_id
WHERE NOT EXISTS (SELECT 1 FROM cost_material_unit WHERE tenant_id=@tenant_id AND unit_code='KG' AND deleted=0);
INSERT INTO cost_material_unit
 (tenant_id,unit_code,unit_name,decimal_scale,status,created_by,updated_by)
SELECT @tenant_id,'L','升',3,1,@user_id,@user_id
WHERE NOT EXISTS (SELECT 1 FROM cost_material_unit WHERE tenant_id=@tenant_id AND unit_code='L' AND deleted=0);

SET @kg_id=(SELECT id FROM cost_material_unit WHERE tenant_id=@tenant_id AND unit_code='KG' AND deleted=0 LIMIT 1);
SET @l_id=(SELECT id FROM cost_material_unit WHERE tenant_id=@tenant_id AND unit_code='L' AND deleted=0 LIMIT 1);

INSERT INTO cost_material
 (tenant_id,material_code,material_name,specification,base_unit_id,source_system,status,created_by,updated_by)
SELECT @tenant_id,'MAT-RICE','东北珍珠米','25kg/袋',@kg_id,'SAAS',1,@user_id,@user_id
WHERE NOT EXISTS (SELECT 1 FROM cost_material WHERE tenant_id=@tenant_id AND material_code='MAT-RICE' AND deleted=0);
INSERT INTO cost_material
 (tenant_id,material_code,material_name,specification,base_unit_id,source_system,status,created_by,updated_by)
SELECT @tenant_id,'MAT-OIL','食用油','20L/桶',@l_id,'SAAS',1,@user_id,@user_id
WHERE NOT EXISTS (SELECT 1 FROM cost_material WHERE tenant_id=@tenant_id AND material_code='MAT-OIL' AND deleted=0);
INSERT INTO cost_material
 (tenant_id,material_code,material_name,specification,base_unit_id,source_system,status,created_by,updated_by)
SELECT @tenant_id,'MAT-SALT','食用盐','500g/袋',@kg_id,'SAAS',1,@user_id,@user_id
WHERE NOT EXISTS (SELECT 1 FROM cost_material WHERE tenant_id=@tenant_id AND material_code='MAT-SALT' AND deleted=0);

INSERT INTO cost_dish
 (tenant_id,dish_code,dish_name,source_system,status,created_by,updated_by)
SELECT @tenant_id,'DISH-RICE','扬州炒饭','SAAS',1,@user_id,@user_id
WHERE NOT EXISTS (SELECT 1 FROM cost_dish WHERE tenant_id=@tenant_id AND dish_code='DISH-RICE' AND deleted=0);
INSERT INTO cost_dish
 (tenant_id,dish_code,dish_name,source_system,status,created_by,updated_by)
SELECT @tenant_id,'DISH-PORK','红烧肉','SAAS',1,@user_id,@user_id
WHERE NOT EXISTS (SELECT 1 FROM cost_dish WHERE tenant_id=@tenant_id AND dish_code='DISH-PORK' AND deleted=0);

INSERT INTO cost_inventory_task
 (tenant_id,store_id,task_code,task_name,status,planned_start_time,planned_end_time,remark,created_by,updated_by)
SELECT @tenant_id,@store_id,'LOCAL-DEMO-001','本地首次盘点','DRAFT',NOW(),DATE_ADD(NOW(),INTERVAL 7 DAY),
 '本地开发演示任务',@user_id,@user_id
WHERE NOT EXISTS (SELECT 1 FROM cost_inventory_task WHERE tenant_id=@tenant_id AND task_code='LOCAL-DEMO-001');
