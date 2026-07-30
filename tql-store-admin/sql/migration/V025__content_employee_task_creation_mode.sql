USE tql_store_saas;

SET NAMES utf8mb4;

SET @column_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'ops_content_employee_task'
      AND COLUMN_NAME = 'creation_mode'
);
SET @sql := IF(@column_exists = 0,
    'ALTER TABLE ops_content_employee_task ADD COLUMN creation_mode VARCHAR(32) NOT NULL DEFAULT ''AI_ASSISTED'' COMMENT ''创作模式快照'' AFTER task_instruction',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE ops_content_employee_task task
JOIN ops_content_plan plan
  ON plan.id = task.plan_id
 AND plan.tenant_id = task.tenant_id
SET task.creation_mode = plan.creation_mode
WHERE task.creation_mode <> plan.creation_mode;
