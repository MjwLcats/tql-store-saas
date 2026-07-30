USE tql_store_saas;

CREATE TABLE IF NOT EXISTS ops_content_video_performance (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    task_id BIGINT DEFAULT NULL,
    account_id BIGINT NOT NULL,
    platform VARCHAR(32) NOT NULL,
    platform_video_id VARCHAR(128) NOT NULL,
    video_title VARCHAR(255) NOT NULL,
    video_url VARCHAR(500) DEFAULT NULL,
    publish_time DATETIME(3) NOT NULL,
    view_count BIGINT NOT NULL DEFAULT 0,
    like_count BIGINT NOT NULL DEFAULT 0,
    comment_count BIGINT NOT NULL DEFAULT 0,
    share_count BIGINT NOT NULL DEFAULT 0,
    favorite_count BIGINT NOT NULL DEFAULT 0,
    follower_gain BIGINT NOT NULL DEFAULT 0,
    conversion_count BIGINT NOT NULL DEFAULT 0,
    transaction_amount DECIMAL(18,2) NOT NULL DEFAULT 0,
    sync_status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    last_sync_time DATETIME(3) DEFAULT NULL,
    create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_video_platform_id (tenant_id, platform, platform_video_id, deleted),
    KEY idx_video_report_publish (tenant_id, publish_time),
    KEY idx_video_report_account (tenant_id, account_id)
) COMMENT='视频发布后开放平台效果明细';
