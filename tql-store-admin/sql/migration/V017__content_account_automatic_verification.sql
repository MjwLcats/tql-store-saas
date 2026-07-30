USE tql_store_saas;

ALTER TABLE ops_content_platform_account
    MODIFY COLUMN status VARCHAR(32) NOT NULL DEFAULT 'PENDING'
    COMMENT '自动校验状态：PENDING待校验、ACTIVE正常、FAILED校验失败、AUTH_EXPIRED授权失效、DISABLED停用';
