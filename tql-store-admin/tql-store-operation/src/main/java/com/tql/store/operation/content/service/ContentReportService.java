package com.tql.store.operation.content.service;

import com.tql.store.operation.content.model.ContentVideoPerformanceView;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ContentReportService {
    private final JdbcTemplate jdbcTemplate;

    public ContentReportService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<ContentVideoPerformanceView> videoPerformance(Long tenantId) {
        return jdbcTemplate.query("""
                SELECT report.id, report.task_id, report.account_id, account.account_name,
                       report.platform, report.platform_video_id, report.video_title, report.video_url,
                       report.publish_time, report.view_count, report.like_count, report.comment_count,
                       report.share_count, report.favorite_count, report.follower_gain,
                       report.conversion_count, report.transaction_amount, report.sync_status,
                       report.last_sync_time
                FROM ops_content_video_performance report
                JOIN ops_content_platform_account account
                  ON account.id = report.account_id AND account.tenant_id = report.tenant_id
                 AND account.deleted = 0
                WHERE report.tenant_id = ? AND report.deleted = 0
                ORDER BY report.publish_time DESC, report.id DESC
                """, (rs, rowNum) -> new ContentVideoPerformanceView(
                rs.getLong("id"), rs.getObject("task_id", Long.class), rs.getLong("account_id"),
                rs.getString("account_name"), rs.getString("platform"),
                rs.getString("platform_video_id"), rs.getString("video_title"),
                rs.getString("video_url"), rs.getTimestamp("publish_time").toLocalDateTime(),
                rs.getLong("view_count"), rs.getLong("like_count"), rs.getLong("comment_count"),
                rs.getLong("share_count"), rs.getLong("favorite_count"), rs.getLong("follower_gain"),
                rs.getLong("conversion_count"), rs.getBigDecimal("transaction_amount"),
                rs.getString("sync_status"),
                rs.getTimestamp("last_sync_time") == null
                        ? null : rs.getTimestamp("last_sync_time").toLocalDateTime()
        ), tenantId);
    }
}
