USE tql_store_saas;

SET NAMES utf8mb4;

SET @column_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'ops_content_plan'
      AND COLUMN_NAME = 'storyboard_count'
);
SET @sql := IF(@column_exists = 0,
    'ALTER TABLE ops_content_plan ADD COLUMN storyboard_count INT NOT NULL DEFAULT 3 COMMENT ''分镜数量，原创任务为1'' AFTER creation_mode',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'ops_content_plan_version'
      AND COLUMN_NAME = 'storyboard_count'
);
SET @sql := IF(@column_exists = 0,
    'ALTER TABLE ops_content_plan_version ADD COLUMN storyboard_count INT NOT NULL DEFAULT 3 COMMENT ''发布时固化的分镜数量'' AFTER creation_mode',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'ops_content_employee_task'
      AND COLUMN_NAME = 'storyboard_count'
);
SET @sql := IF(@column_exists = 0,
    'ALTER TABLE ops_content_employee_task ADD COLUMN storyboard_count INT NOT NULL DEFAULT 3 COMMENT ''分镜数量快照，原创任务为1'' AFTER task_instruction',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE ops_content_plan
SET storyboard_count = CASE
    WHEN task_instruction LIKE '%分镜要求%| 8.%' OR task_instruction LIKE '%分镜要求%|8.%' OR task_instruction LIKE '%分镜要求%｜ 8.%' OR task_instruction LIKE '%分镜要求%｜8.%' THEN 8
    WHEN task_instruction LIKE '%分镜要求%| 7.%' OR task_instruction LIKE '%分镜要求%|7.%' OR task_instruction LIKE '%分镜要求%｜ 7.%' OR task_instruction LIKE '%分镜要求%｜7.%' THEN 7
    WHEN task_instruction LIKE '%分镜要求%| 6.%' OR task_instruction LIKE '%分镜要求%|6.%' OR task_instruction LIKE '%分镜要求%｜ 6.%' OR task_instruction LIKE '%分镜要求%｜6.%' THEN 6
    WHEN task_instruction LIKE '%分镜要求%| 5.%' OR task_instruction LIKE '%分镜要求%|5.%' OR task_instruction LIKE '%分镜要求%｜ 5.%' OR task_instruction LIKE '%分镜要求%｜5.%' THEN 5
    WHEN task_instruction LIKE '%分镜要求%| 4.%' OR task_instruction LIKE '%分镜要求%|4.%' OR task_instruction LIKE '%分镜要求%｜ 4.%' OR task_instruction LIKE '%分镜要求%｜4.%' THEN 4
    WHEN task_instruction LIKE '%分镜要求%| 3.%' OR task_instruction LIKE '%分镜要求%|3.%' OR task_instruction LIKE '%分镜要求%｜ 3.%' OR task_instruction LIKE '%分镜要求%｜3.%' THEN 3
    WHEN task_instruction LIKE '%分镜要求%| 2.%' OR task_instruction LIKE '%分镜要求%|2.%' OR task_instruction LIKE '%分镜要求%｜ 2.%' OR task_instruction LIKE '%分镜要求%｜2.%' THEN 2
    WHEN task_instruction LIKE '%分镜要求%' THEN 1
    ELSE storyboard_count
END
WHERE creation_mode <> 'SELF_CREATED'
  AND task_instruction LIKE '%分镜要求%';

UPDATE ops_content_plan_version
SET storyboard_count = CASE
    WHEN task_instruction LIKE '%分镜要求%| 8.%' OR task_instruction LIKE '%分镜要求%|8.%' OR task_instruction LIKE '%分镜要求%｜ 8.%' OR task_instruction LIKE '%分镜要求%｜8.%' THEN 8
    WHEN task_instruction LIKE '%分镜要求%| 7.%' OR task_instruction LIKE '%分镜要求%|7.%' OR task_instruction LIKE '%分镜要求%｜ 7.%' OR task_instruction LIKE '%分镜要求%｜7.%' THEN 7
    WHEN task_instruction LIKE '%分镜要求%| 6.%' OR task_instruction LIKE '%分镜要求%|6.%' OR task_instruction LIKE '%分镜要求%｜ 6.%' OR task_instruction LIKE '%分镜要求%｜6.%' THEN 6
    WHEN task_instruction LIKE '%分镜要求%| 5.%' OR task_instruction LIKE '%分镜要求%|5.%' OR task_instruction LIKE '%分镜要求%｜ 5.%' OR task_instruction LIKE '%分镜要求%｜5.%' THEN 5
    WHEN task_instruction LIKE '%分镜要求%| 4.%' OR task_instruction LIKE '%分镜要求%|4.%' OR task_instruction LIKE '%分镜要求%｜ 4.%' OR task_instruction LIKE '%分镜要求%｜4.%' THEN 4
    WHEN task_instruction LIKE '%分镜要求%| 3.%' OR task_instruction LIKE '%分镜要求%|3.%' OR task_instruction LIKE '%分镜要求%｜ 3.%' OR task_instruction LIKE '%分镜要求%｜3.%' THEN 3
    WHEN task_instruction LIKE '%分镜要求%| 2.%' OR task_instruction LIKE '%分镜要求%|2.%' OR task_instruction LIKE '%分镜要求%｜ 2.%' OR task_instruction LIKE '%分镜要求%｜2.%' THEN 2
    WHEN task_instruction LIKE '%分镜要求%' THEN 1
    ELSE storyboard_count
END
WHERE creation_mode <> 'SELF_CREATED'
  AND task_instruction LIKE '%分镜要求%';

UPDATE ops_content_plan
SET storyboard_count = 1
WHERE creation_mode = 'SELF_CREATED'
  AND storyboard_count <> 1;

UPDATE ops_content_plan_version
SET storyboard_count = 1
WHERE creation_mode = 'SELF_CREATED'
  AND storyboard_count <> 1;

UPDATE ops_content_employee_task task
JOIN ops_content_plan plan
  ON plan.id = task.plan_id
 AND plan.tenant_id = task.tenant_id
SET task.storyboard_count = plan.storyboard_count
WHERE task.storyboard_count <> plan.storyboard_count;
