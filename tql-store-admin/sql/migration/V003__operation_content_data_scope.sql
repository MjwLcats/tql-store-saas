USE tql_store_saas;
SET NAMES utf8mb4;

ALTER TABLE ops_content
    ADD COLUMN store_id BIGINT NULL COMMENT '所属门店ID' AFTER tenant_id,
    ADD KEY idx_content_store (tenant_id, store_id);

UPDATE ops_content SET store_id = CASE id
    WHEN 1001 THEN 11002
    WHEN 1002 THEN 11003
    WHEN 1003 THEN 11001
    WHEN 1004 THEN 11004
    WHEN 1005 THEN 11001
    WHEN 1006 THEN 11002
    WHEN 1007 THEN 11003
    WHEN 1008 THEN 11004
    ELSE store_id
END
WHERE tenant_id = 10001;
