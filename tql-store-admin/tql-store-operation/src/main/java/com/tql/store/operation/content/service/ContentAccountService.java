package com.tql.store.operation.content.service;

import com.tql.store.operation.content.model.ContentAccountSaveRequest;
import com.tql.store.operation.content.model.ContentAccountView;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Set;

@Service
public class ContentAccountService {
    private static final Set<String> PLATFORMS = Set.of("抖音", "快手", "小红书", "视频号");
    private static final Set<String> TYPES = Set.of("蓝V", "职人", "个人");
    private final JdbcTemplate jdbcTemplate;

    public ContentAccountService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<ContentAccountView> list(Long tenantId) {
        return jdbcTemplate.query("""
                SELECT account.id, account.platform, account.account_name,
                       account.platform_account_id, account.platform_homepage_url, account.account_type,
                       account.organization_id, organization.org_name,
                       account.employee_id, employee.display_name, employee.employee_number,
                       account.status, account.update_time
                FROM ops_content_platform_account account
                LEFT JOIN sys_merchant_organization organization
                  ON organization.id = account.organization_id AND organization.tenant_id = account.tenant_id
                LEFT JOIN sys_merchant_user employee
                  ON employee.id = account.employee_id AND employee.tenant_id = account.tenant_id
                WHERE account.tenant_id = ? AND account.deleted = 0
                ORDER BY account.id DESC
                """, (rs, rowNum) -> new ContentAccountView(
                rs.getLong("id"), rs.getString("platform"), rs.getString("account_name"),
                rs.getString("platform_account_id"), rs.getString("platform_homepage_url"), rs.getString("account_type"),
                rs.getObject("organization_id", Long.class), rs.getString("org_name"),
                rs.getLong("employee_id"), rs.getString("display_name"),
                rs.getString("employee_number"), rs.getString("status"),
                rs.getTimestamp("update_time").toLocalDateTime()
        ), tenantId);
    }

    @Transactional
    public Long create(Long tenantId, Long operatorId, ContentAccountSaveRequest request) {
        validate(tenantId, request);
        var keyHolder = new org.springframework.jdbc.support.GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO ops_content_platform_account
                      (tenant_id, platform, account_name, platform_account_id, platform_homepage_url, account_type,
                       organization_id, employee_id, status, create_by, update_by)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """, Statement.RETURN_GENERATED_KEYS);
            statement.setObject(1, tenantId);
            statement.setObject(2, request.platform().trim());
            statement.setObject(3, request.accountName().trim());
            statement.setObject(4, request.platformAccountId().trim());
            statement.setObject(5, normalizeUrl(request.platformHomepageUrl()));
            statement.setObject(6, request.accountType().trim());
            statement.setObject(7, request.organizationId());
            statement.setObject(8, request.employeeId());
            statement.setObject(9, "PENDING");
            statement.setObject(10, operatorId);
            statement.setObject(11, operatorId);
            return statement;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }

    @Transactional
    public void update(Long tenantId, Long operatorId, Long id, ContentAccountSaveRequest request) {
        validate(tenantId, request);
        int changed = jdbcTemplate.update("""
                UPDATE ops_content_platform_account
                SET platform = ?, account_name = ?, platform_account_id = ?, platform_homepage_url = ?, account_type = ?,
                    organization_id = ?, employee_id = ?, update_by = ?
                WHERE id = ? AND tenant_id = ? AND deleted = 0
                """, request.platform().trim(), request.accountName().trim(),
                request.platformAccountId().trim(), normalizeUrl(request.platformHomepageUrl()), request.accountType().trim(),
                request.organizationId(), request.employeeId(), operatorId, id, tenantId);
        if (changed == 0) throw new IllegalArgumentException("账号不存在");
    }

    @Transactional
    public int importRecords(Long tenantId, Long operatorId, List<ContentAccountSaveRequest> records) {
        int count = 0;
        for (ContentAccountSaveRequest record : records) {
            create(tenantId, operatorId, record);
            count++;
        }
        return count;
    }

    @Transactional
    public void delete(Long tenantId, Long operatorId, List<Long> ids) {
        if (ids == null || ids.isEmpty()) throw new IllegalArgumentException("请选择需要删除的账号");
        String placeholders = String.join(",", ids.stream().map(id -> "?").toList());
        Object[] args = new Object[ids.size() + 2];
        args[0] = operatorId;
        for (int i = 0; i < ids.size(); i++) args[i + 1] = ids.get(i);
        args[args.length - 1] = tenantId;
        jdbcTemplate.update("UPDATE ops_content_platform_account SET deleted = 1, update_by = ? " +
                "WHERE id IN (" + placeholders + ") AND tenant_id = ? AND deleted = 0", args);
    }

    private void validate(Long tenantId, ContentAccountSaveRequest request) {
        if (!PLATFORMS.contains(request.platform())) throw new IllegalArgumentException("不支持的发布平台");
        if (!TYPES.contains(request.accountType())) throw new IllegalArgumentException("不支持的账号类型");
        Integer employeeCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM sys_merchant_user
                WHERE id = ? AND tenant_id = ? AND status = 1 AND login_enabled = 1 AND deleted = 0
                """, Integer.class, request.employeeId(), tenantId);
        if (employeeCount == null || employeeCount == 0) throw new IllegalArgumentException("归属员工不可用");
        if (request.organizationId() != null) {
            Integer organizationCount = jdbcTemplate.queryForObject("""
                    SELECT COUNT(*) FROM sys_merchant_organization
                    WHERE id = ? AND tenant_id = ? AND status = 1 AND deleted = 0
                    """, Integer.class, request.organizationId(), tenantId);
            if (organizationCount == null || organizationCount == 0) {
                throw new IllegalArgumentException("归属组织不可用");
            }
        }
    }

    private String normalizeUrl(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
