package com.tql.store.system.service;

import com.tql.store.common.api.PageResult;
import com.tql.store.common.security.PasswordHasher;
import com.tql.store.system.model.MenuView;
import com.tql.store.system.model.OrganizationOption;
import com.tql.store.system.model.RoleSaveRequest;
import com.tql.store.system.model.RoleView;
import com.tql.store.system.model.StoreOption;
import com.tql.store.system.model.UserDetail;
import com.tql.store.system.model.UserSaveRequest;
import com.tql.store.system.model.UserView;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class RbacService {

    private static final Set<String> DATA_SCOPES = Set.of(
            "ALL", "DEPT_AND_CHILD", "DEPT", "STORE_AND_CHILD", "STORE", "SELF", "CUSTOM");

    private final JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate namedJdbcTemplate;

    public RbacService(JdbcTemplate jdbcTemplate, NamedParameterJdbcTemplate namedJdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.namedJdbcTemplate = namedJdbcTemplate;
    }

    public void requirePermission(Long userId, Long tenantId, String clientType, String resource) {
        ClientSchema schema = clientSchema(clientType);
        String permission = permissionPrefix(clientType) + ":system:" + resource + ":view";
        String sql = schema.merchant() ? """
                SELECT COUNT(*)
                FROM sys_merchant_user_role ur
                JOIN sys_merchant_user u
                  ON u.id = ur.merchant_user_id AND u.tenant_id = ?
                 AND u.status = 1 AND u.login_enabled = 1 AND u.deleted = 0
                JOIN sys_role r ON r.id = ur.role_id AND r.status = 1
                JOIN sys_role_menu rm ON rm.role_id = r.id
                JOIN sys_menu m ON m.id = rm.menu_id AND m.visible = 1
                WHERE ur.merchant_user_id = ? AND r.tenant_id = ? AND r.client_type = 'MERCHANT'
                  AND m.permission_code = ?
                """ : """
                SELECT COUNT(*)
                FROM sys_platform_user_role ur
                JOIN sys_platform_user u
                  ON u.id = ur.platform_user_id AND u.status = 1 AND u.deleted = 0
                JOIN sys_role r ON r.id = ur.role_id AND r.status = 1
                JOIN sys_role_menu rm ON rm.role_id = r.id
                JOIN sys_menu m ON m.id = rm.menu_id AND m.visible = 1
                WHERE ur.platform_user_id = ? AND r.tenant_id = 0 AND r.client_type = 'PLATFORM'
                  AND m.permission_code = ?
                """;
        Object[] params = schema.merchant()
                ? new Object[]{tenantId, userId, tenantId, permission}
                : new Object[]{userId, permission};
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, params);
        if (count == null || count == 0) {
            throw new SecurityException("无权执行该操作");
        }
    }

    public PageResult<UserView> listUsers(
            Long tenantId, String clientType, String keyword, Integer status,
            Long storeId, Long organizationId, int page, int pageSize) {
        ClientSchema schema = clientSchema(clientType);
        int safePage = Math.max(page, 1);
        int safePageSize = Math.min(Math.max(pageSize, 1), 100);
        StringBuilder where = new StringBuilder(schema.merchant()
                ? " WHERE u.tenant_id = :tenantId AND u.directory_visible = 1 AND u.deleted = 0 "
                : " WHERE u.deleted = 0 ");
        MapSqlParameterSource params = new MapSqlParameterSource();
        if (schema.merchant()) params.addValue("tenantId", tenantId);
        if (keyword != null && !keyword.isBlank()) {
            where.append(schema.merchant()
                    ? " AND (u.username LIKE :keyword OR u.employee_number LIKE :keyword OR u.display_name LIKE :keyword OR u.phone LIKE :keyword) "
                    : " AND (u.username LIKE :keyword OR u.display_name LIKE :keyword OR u.phone LIKE :keyword) ");
            params.addValue("keyword", "%" + keyword.trim() + "%");
        }
        if (status != null) {
            where.append(" AND u.status = :status ");
            params.addValue("status", status);
        }
        if (schema.merchant() && storeId != null) {
            where.append(" AND EXISTS (SELECT 1 FROM sys_merchant_user_store usq "
                    + "WHERE usq.merchant_user_id = u.id AND usq.store_id = :storeId) ");
            params.addValue("storeId", storeId);
        }
        if (organizationId != null) {
            validateOrganizationId(schema, organizationId, tenantId);
            where.append(" AND u.organization_id IN (:organizationIds) ");
            params.addValue("organizationIds", listOrganizationScopeIds(schema, organizationId, tenantId));
        }

        Long total = namedJdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + schema.userTable() + " u" + where, params, Long.class);
        params.addValue("offset", (safePage - 1) * safePageSize);
        params.addValue("pageSize", safePageSize);
        String sql = schema.merchant() ? """
                SELECT u.id, u.username, u.employee_number, u.display_name,
                       organization.org_name AS organization_name, u.email, u.phone,
                       u.login_enabled, u.source_type, u.status, u.data_scope,
                       u.primary_store_id, store_record.store_name AS primary_store_name,
                       GROUP_CONCAT(DISTINCT role_record.role_name ORDER BY role_record.role_name SEPARATOR ',') AS role_names
                FROM sys_merchant_user u
                LEFT JOIN sys_merchant_organization organization
                  ON organization.id = u.organization_id AND organization.tenant_id = u.tenant_id
                LEFT JOIN sys_store store_record
                  ON store_record.id = u.primary_store_id AND store_record.tenant_id = u.tenant_id
                LEFT JOIN sys_merchant_user_role user_role ON user_role.merchant_user_id = u.id
                LEFT JOIN sys_role role_record
                  ON role_record.id = user_role.role_id AND role_record.tenant_id = u.tenant_id
                """ + where + """
                GROUP BY u.id, u.username, u.employee_number, u.display_name, organization.org_name,
                         u.email, u.phone, u.login_enabled, u.source_type, u.status, u.data_scope,
                         u.primary_store_id, store_record.store_name
                ORDER BY u.id DESC LIMIT :offset, :pageSize
                """ : """
                SELECT u.id, u.username, NULL AS employee_number, u.display_name,
                       organization.org_name AS organization_name, u.email, u.phone,
                       1 AS login_enabled, 'LOCAL' AS source_type, u.status, u.data_scope,
                       NULL AS primary_store_id, NULL AS primary_store_name,
                       GROUP_CONCAT(DISTINCT role_record.role_name ORDER BY role_record.role_name SEPARATOR ',') AS role_names
                FROM sys_platform_user u
                LEFT JOIN sys_platform_organization organization ON organization.id = u.organization_id
                LEFT JOIN sys_platform_user_role user_role ON user_role.platform_user_id = u.id
                LEFT JOIN sys_role role_record
                  ON role_record.id = user_role.role_id AND role_record.tenant_id = 0
                """ + where + """
                GROUP BY u.id, u.username, u.display_name, organization.org_name,
                         u.email, u.phone, u.status, u.data_scope
                ORDER BY u.id DESC LIMIT :offset, :pageSize
                """;
        List<UserView> records = namedJdbcTemplate.query(sql, params, (rs, rowNum) -> new UserView(
                rs.getLong("id"),
                rs.getString("username"),
                rs.getString("employee_number"),
                rs.getString("display_name"),
                rs.getString("organization_name"),
                rs.getString("email"),
                rs.getString("phone"),
                rs.getInt("login_enabled") == 1,
                rs.getString("source_type"),
                rs.getInt("status"),
                rs.getString("data_scope"),
                rs.getObject("primary_store_id", Long.class),
                rs.getString("primary_store_name"),
                splitNames(rs.getString("role_names"))
        ));
        return new PageResult<>(records, total == null ? 0 : total, safePage, safePageSize);
    }

    public UserDetail getUser(Long id, Long tenantId, String clientType) {
        ClientSchema schema = clientSchema(clientType);
        String sql = schema.merchant() ? """
                SELECT u.id, u.username, u.employee_number, u.login_enabled, u.source_type,
                       u.organization_id, organization.org_name AS organization_name,
                       u.display_name, u.email, u.phone, u.status, u.data_scope, u.primary_store_id
                FROM sys_merchant_user u
                LEFT JOIN sys_merchant_organization organization
                  ON organization.id = u.organization_id AND organization.tenant_id = u.tenant_id
                WHERE u.id = ? AND u.tenant_id = ? AND u.deleted = 0
                """ : """
                SELECT u.id, u.username, NULL AS employee_number, 1 AS login_enabled, 'LOCAL' AS source_type,
                       u.organization_id, organization.org_name AS organization_name,
                       u.display_name, u.email, u.phone, u.status, u.data_scope, NULL AS primary_store_id
                FROM sys_platform_user u
                LEFT JOIN sys_platform_organization organization ON organization.id = u.organization_id
                WHERE u.id = ? AND u.deleted = 0
                """;
        Object[] params = schema.merchant() ? new Object[]{id, tenantId} : new Object[]{id};
        UserDetail base = jdbcTemplate.queryForObject(sql, (rs, rowNum) -> new UserDetail(
                rs.getLong("id"), rs.getString("username"), rs.getString("employee_number"),
                rs.getInt("login_enabled") == 1, rs.getString("source_type"),
                rs.getObject("organization_id", Long.class), rs.getString("organization_name"),
                rs.getString("display_name"), rs.getString("email"), rs.getString("phone"),
                rs.getInt("status"), rs.getString("data_scope"),
                rs.getObject("primary_store_id", Long.class), List.of(), List.of()
        ), params);
        if (base == null) throw new IllegalArgumentException("用户不存在");
        List<Long> roleIds = jdbcTemplate.queryForList(
                "SELECT role_id FROM " + schema.userRoleTable()
                        + " WHERE " + schema.userIdColumn() + " = ? ORDER BY role_id", Long.class, id);
        List<Long> storeIds = schema.merchant() ? jdbcTemplate.queryForList(
                "SELECT store_id FROM sys_merchant_user_store WHERE merchant_user_id = ? ORDER BY store_id",
                Long.class, id) : List.of();
        return new UserDetail(base.id(), base.username(), base.employeeNumber(), base.loginEnabled(),
                base.sourceType(), base.organizationId(), base.organizationName(), base.displayName(),
                base.email(), base.phone(), base.status(), base.dataScope(), base.primaryStoreId(),
                roleIds, storeIds);
    }

    @Transactional
    public Long createUser(UserSaveRequest request, Long tenantId, String clientType) {
        ClientSchema schema = clientSchema(clientType);
        UserInput input = validateUser(request, tenantId, clientType, null, true);
        String passwordHash = trimToNull(request.password()) == null ? null : PasswordHasher.encode(request.password());
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement;
            if (schema.merchant()) {
                statement = connection.prepareStatement("""
                        INSERT INTO sys_merchant_user
                            (tenant_id, organization_id, primary_store_id, username, password_hash,
                             display_name, email, phone, source_type, login_enabled, data_scope, status, deleted)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'LOCAL', ?, ?, ?, 0)
                        """, Statement.RETURN_GENERATED_KEYS);
                statement.setLong(1, tenantId);
                statement.setObject(2, input.organizationId());
                statement.setObject(3, input.primaryStoreId());
                statement.setString(4, input.username());
                statement.setString(5, passwordHash);
                statement.setString(6, input.displayName());
                statement.setString(7, input.email());
                statement.setString(8, input.phone());
                statement.setInt(9, input.loginEnabled() ? 1 : 0);
                statement.setString(10, input.dataScope());
                statement.setInt(11, input.status());
            } else {
                statement = connection.prepareStatement("""
                        INSERT INTO sys_platform_user
                            (organization_id, username, password_hash, display_name,
                             email, phone, data_scope, status, deleted)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, 0)
                        """, Statement.RETURN_GENERATED_KEYS);
                statement.setObject(1, input.organizationId());
                statement.setString(2, input.username());
                statement.setString(3, passwordHash);
                statement.setString(4, input.displayName());
                statement.setString(5, input.email());
                statement.setString(6, input.phone());
                statement.setString(7, input.dataScope());
                statement.setInt(8, input.status());
            }
            return statement;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key == null) throw new IllegalStateException("用户创建失败");
        long userId = key.longValue();
        replaceUserRoles(schema, userId, input.roleIds());
        replaceUserStores(schema, userId, input.storeIds());
        return userId;
    }

    @Transactional
    public void updateUser(Long id, UserSaveRequest request, Long tenantId, String clientType) {
        ClientSchema schema = clientSchema(clientType);
        assertUserExists(id, tenantId, clientType);
        UserInput input = validateUser(request, tenantId, clientType, id, false);
        boolean updatePassword = request.password() != null && !request.password().isBlank();
        if (updatePassword) validatePassword(request.password());
        if (schema.merchant()) {
            if (updatePassword) {
                jdbcTemplate.update("""
                        UPDATE sys_merchant_user
                        SET organization_id = ?, primary_store_id = ?, username = ?, password_hash = ?,
                            display_name = ?, email = ?, phone = ?, login_enabled = ?, data_scope = ?, status = ?
                        WHERE id = ? AND tenant_id = ? AND deleted = 0
                        """, input.organizationId(), input.primaryStoreId(), input.username(),
                        PasswordHasher.encode(request.password()), input.displayName(), input.email(), input.phone(),
                        input.loginEnabled() ? 1 : 0, input.dataScope(), input.status(), id, tenantId);
            } else {
                jdbcTemplate.update("""
                        UPDATE sys_merchant_user
                        SET organization_id = ?, primary_store_id = ?, username = ?, display_name = ?,
                            email = ?, phone = ?, login_enabled = ?, data_scope = ?, status = ?
                        WHERE id = ? AND tenant_id = ? AND deleted = 0
                        """, input.organizationId(), input.primaryStoreId(), input.username(),
                        input.displayName(), input.email(), input.phone(), input.loginEnabled() ? 1 : 0,
                        input.dataScope(), input.status(), id, tenantId);
            }
        } else if (updatePassword) {
            jdbcTemplate.update("""
                    UPDATE sys_platform_user
                    SET organization_id = ?, username = ?, password_hash = ?, display_name = ?,
                        email = ?, phone = ?, data_scope = ?, status = ?
                    WHERE id = ? AND deleted = 0
                    """, input.organizationId(), input.username(), PasswordHasher.encode(request.password()),
                    input.displayName(), input.email(), input.phone(), input.dataScope(), input.status(), id);
        } else {
            jdbcTemplate.update("""
                    UPDATE sys_platform_user
                    SET organization_id = ?, username = ?, display_name = ?,
                        email = ?, phone = ?, data_scope = ?, status = ?
                    WHERE id = ? AND deleted = 0
                    """, input.organizationId(), input.username(), input.displayName(), input.email(),
                    input.phone(), input.dataScope(), input.status(), id);
        }
        replaceUserRoles(schema, id, input.roleIds());
        replaceUserStores(schema, id, input.storeIds());
    }

    @Transactional
    public void deleteUser(Long id, Long operatorId, Long tenantId, String clientType) {
        ClientSchema schema = clientSchema(clientType);
        if (id.equals(operatorId)) throw new IllegalArgumentException("不能删除当前登录用户");
        assertUserExists(id, tenantId, clientType);
        if (schema.merchant()) {
            String sourceType = jdbcTemplate.queryForObject(
                    "SELECT source_type FROM sys_merchant_user WHERE id = ? AND tenant_id = ?",
                    String.class, id, tenantId);
            if ("HR_BUTLER".equalsIgnoreCase(sourceType)) {
                throw new IllegalArgumentException("人力管家同步用户不能删除，可以关闭登录或停用人员");
            }
        }
        replaceUserRoles(schema, id, List.of());
        replaceUserStores(schema, id, List.of());
        if (schema.merchant()) {
            jdbcTemplate.update("DELETE FROM sys_merchant_user WHERE id = ? AND tenant_id = ?", id, tenantId);
        } else {
            jdbcTemplate.update("DELETE FROM sys_platform_user WHERE id = ?", id);
        }
    }

    public List<RoleView> listRoles(Long tenantId, String clientType, String keyword, Integer status) {
        ClientSchema schema = clientSchema(clientType);
        StringBuilder sql = new StringBuilder("""
                SELECT r.id, r.role_code, r.role_name, r.status, r.remark,
                       (SELECT COUNT(*) FROM sys_role_menu rm WHERE rm.role_id = r.id) AS menu_count,
                       (SELECT COUNT(*) FROM %s ur WHERE ur.role_id = r.id) AS user_count
                FROM sys_role r
                WHERE r.tenant_id = :tenantId AND r.client_type = :clientType
                """.formatted(schema.userRoleTable()));
        MapSqlParameterSource params = contextParams(tenantId, clientType);
        if (keyword != null && !keyword.isBlank()) {
            sql.append(" AND (r.role_code LIKE :keyword OR r.role_name LIKE :keyword) ");
            params.addValue("keyword", "%" + keyword.trim() + "%");
        }
        if (status != null) {
            sql.append(" AND r.status = :status ");
            params.addValue("status", status);
        }
        sql.append(" ORDER BY r.id ");
        return namedJdbcTemplate.query(sql.toString(), params, (rs, rowNum) -> {
            long roleId = rs.getLong("id");
            List<Long> menuIds = jdbcTemplate.queryForList(
                    "SELECT menu_id FROM sys_role_menu WHERE role_id = ? ORDER BY menu_id", Long.class, roleId);
            return new RoleView(roleId, rs.getString("role_code"), rs.getString("role_name"),
                    rs.getInt("status"), rs.getString("remark"), menuIds,
                    rs.getLong("menu_count"), rs.getLong("user_count"));
        });
    }

    @Transactional
    public Long createRole(RoleSaveRequest request, Long tenantId, String clientType) {
        RoleInput input = validateRole(request, tenantId, clientType, null);
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO sys_role (tenant_id, role_code, role_name, client_type, status, remark)
                    VALUES (?, ?, ?, ?, ?, ?)
                    """, Statement.RETURN_GENERATED_KEYS);
            statement.setLong(1, tenantId);
            statement.setString(2, input.code());
            statement.setString(3, input.name());
            statement.setString(4, normalizeClient(clientType));
            statement.setInt(5, input.status());
            statement.setString(6, input.remark());
            return statement;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key == null) throw new IllegalStateException("角色创建失败");
        long roleId = key.longValue();
        replaceRoleMenus(roleId, input.menuIds());
        return roleId;
    }

    @Transactional
    public void updateRole(Long id, RoleSaveRequest request, Long tenantId, String clientType) {
        assertRoleExists(id, tenantId, clientType);
        RoleInput input = validateRole(request, tenantId, clientType, id);
        jdbcTemplate.update("""
                UPDATE sys_role SET role_code = ?, role_name = ?, status = ?, remark = ?
                WHERE id = ? AND tenant_id = ? AND client_type = ?
                """, input.code(), input.name(), input.status(), input.remark(),
                id, tenantId, normalizeClient(clientType));
        replaceRoleMenus(id, input.menuIds());
    }

    @Transactional
    public void deleteRole(Long id, Long tenantId, String clientType) {
        ClientSchema schema = clientSchema(clientType);
        assertRoleExists(id, tenantId, clientType);
        Integer userCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + schema.userRoleTable() + " WHERE role_id = ?", Integer.class, id);
        if (userCount != null && userCount > 0) {
            throw new IllegalArgumentException("该角色仍有关联用户，不能删除");
        }
        jdbcTemplate.update("DELETE FROM sys_role_menu WHERE role_id = ?", id);
        jdbcTemplate.update("DELETE FROM sys_role WHERE id = ? AND tenant_id = ? AND client_type = ?",
                id, tenantId, normalizeClient(clientType));
    }

    public List<StoreOption> listStores(Long tenantId) {
        return jdbcTemplate.query("""
                SELECT id, parent_id, store_code, store_name
                FROM sys_store
                WHERE tenant_id = ? AND status = 1
                ORDER BY sort_order, id
                """, (rs, rowNum) -> new StoreOption(
                rs.getLong("id"), rs.getLong("parent_id"),
                rs.getString("store_code"), rs.getString("store_name")
        ), tenantId);
    }

    public List<OrganizationOption> listOrganizations(Long tenantId, String clientType) {
        ClientSchema schema = clientSchema(clientType);
        String sql = schema.merchant() ? """
                SELECT id, parent_id, org_code, org_name, status
                FROM sys_merchant_organization
                WHERE tenant_id = ? AND deleted = 0
                ORDER BY sort_order, id
                """ : """
                SELECT id, parent_id, org_code, org_name, status
                FROM sys_platform_organization
                WHERE deleted = 0
                ORDER BY sort_order, id
                """;
        Object[] params = schema.merchant() ? new Object[]{tenantId} : new Object[]{};
        return jdbcTemplate.query(sql, (rs, rowNum) -> new OrganizationOption(
                rs.getLong("id"), rs.getLong("parent_id"), rs.getString("org_code"),
                rs.getString("org_name"), rs.getInt("status") != 1
        ), params);
    }

    private List<Long> listOrganizationScopeIds(ClientSchema schema, Long organizationId, Long tenantId) {
        String sql = schema.merchant() ? """
                WITH RECURSIVE organization_tree AS (
                    SELECT id FROM sys_merchant_organization
                    WHERE id = ? AND tenant_id = ? AND deleted = 0
                    UNION ALL
                    SELECT child.id FROM sys_merchant_organization child
                    JOIN organization_tree parent ON child.parent_id = parent.id
                    WHERE child.tenant_id = ? AND child.deleted = 0
                )
                SELECT id FROM organization_tree
                """ : """
                WITH RECURSIVE organization_tree AS (
                    SELECT id FROM sys_platform_organization WHERE id = ? AND deleted = 0
                    UNION ALL
                    SELECT child.id FROM sys_platform_organization child
                    JOIN organization_tree parent ON child.parent_id = parent.id
                    WHERE child.deleted = 0
                )
                SELECT id FROM organization_tree
                """;
        Object[] params = schema.merchant()
                ? new Object[]{organizationId, tenantId, tenantId}
                : new Object[]{organizationId};
        return jdbcTemplate.query(sql, (rs, rowNum) -> rs.getLong("id"), params);
    }

    public List<MenuView> listAssignableMenus(Long tenantId, String clientType) {
        return jdbcTemplate.query("""
                SELECT id, menu_name, route_path, component_key, icon, permission_code, sort_order
                FROM sys_menu
                WHERE tenant_id = ? AND client_type = ? AND visible = 1
                ORDER BY sort_order, id
                """, (rs, rowNum) -> new MenuView(
                rs.getLong("id"), rs.getString("menu_name"), rs.getString("route_path"),
                rs.getString("component_key"), rs.getString("icon"),
                rs.getString("permission_code"), rs.getInt("sort_order")
        ), tenantId, normalizeClient(clientType));
    }

    private UserInput validateUser(
            UserSaveRequest request, Long tenantId, String clientType, Long currentId, boolean creating) {
        if (request == null) throw new IllegalArgumentException("用户参数不能为空");
        ClientSchema schema = clientSchema(clientType);
        boolean loginEnabled = !schema.merchant() || Boolean.TRUE.equals(request.loginEnabled());
        String username = trimToNull(request.username());
        if (loginEnabled) username = requireText(username, "登录账号", 64);
        if (username != null && username.length() > 64) throw new IllegalArgumentException("登录账号长度不能超过 64 位");
        String displayName = requireText(request.displayName(), "用户姓名", 64);
        int status = normalizeStatus(request.status());
        String dataScope = normalizeDataScope(request.dataScope());

        boolean hasPassword = currentId != null && userHasPassword(schema, currentId, tenantId);
        if (request.password() != null && !request.password().isBlank()) validatePassword(request.password());
        if (loginEnabled && (creating || !hasPassword) && trimToNull(request.password()) == null) {
            throw new IllegalArgumentException("开通登录必须设置 8-64 位密码");
        }

        if (username != null) {
            Integer duplicate = schema.merchant()
                    ? jdbcTemplate.queryForObject("""
                            SELECT COUNT(*) FROM sys_merchant_user
                            WHERE tenant_id = ? AND username = ? AND (? IS NULL OR id <> ?)
                            """, Integer.class, tenantId, username, currentId, currentId)
                    : jdbcTemplate.queryForObject("""
                            SELECT COUNT(*) FROM sys_platform_user
                            WHERE username = ? AND (? IS NULL OR id <> ?)
                            """, Integer.class, username, currentId, currentId);
            if (duplicate != null && duplicate > 0) throw new IllegalArgumentException("登录账号已存在");
        }

        List<Long> roleIds = distinctIds(request.roleIds());
        if (loginEnabled && roleIds.isEmpty()) throw new IllegalArgumentException("开通登录必须至少选择一个角色");
        validateOwnedIds("sys_role", roleIds, tenantId, clientType, "角色");

        Long organizationId = request.organizationId();
        validateOrganizationId(schema, organizationId, tenantId);
        Long primaryStoreId = request.primaryStoreId();
        List<Long> storeIds = new ArrayList<>(distinctIds(request.storeIds()));
        if (schema.merchant()) {
            if (loginEnabled && primaryStoreId == null) throw new IllegalArgumentException("开通登录必须选择所属主门店");
            if (primaryStoreId != null && !storeIds.contains(primaryStoreId)) storeIds.add(primaryStoreId);
            validateStoreIds(storeIds, tenantId);
            if (loginEnabled && "CUSTOM".equals(dataScope) && storeIds.isEmpty()) {
                throw new IllegalArgumentException("自定义数据权限必须选择授权门店");
            }
        } else {
            primaryStoreId = null;
            storeIds.clear();
        }
        return new UserInput(username, displayName, trimToNull(request.email()), trimToNull(request.phone()),
                loginEnabled, status, dataScope, organizationId, primaryStoreId, roleIds, storeIds);
    }

    private RoleInput validateRole(
            RoleSaveRequest request, Long tenantId, String clientType, Long currentId) {
        if (request == null) throw new IllegalArgumentException("角色参数不能为空");
        String code = requireText(request.roleCode(), "角色编码", 64).toUpperCase(Locale.ROOT);
        if (!code.matches("[A-Z][A-Z0-9_]*")) {
            throw new IllegalArgumentException("角色编码只能使用大写字母、数字和下划线");
        }
        String name = requireText(request.roleName(), "角色名称", 64);
        int status = normalizeStatus(request.status());
        Integer duplicate = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM sys_role
                WHERE tenant_id = ? AND client_type = ? AND role_code = ? AND (? IS NULL OR id <> ?)
                """, Integer.class, tenantId, normalizeClient(clientType), code, currentId, currentId);
        if (duplicate != null && duplicate > 0) throw new IllegalArgumentException("角色编码已存在");
        List<Long> menuIds = distinctIds(request.menuIds());
        validateOwnedIds("sys_menu", menuIds, tenantId, clientType, "菜单");
        return new RoleInput(code, name, status, trimToNull(request.remark()), menuIds);
    }

    private void validateOwnedIds(
            String table, List<Long> ids, Long tenantId, String clientType, String label) {
        if (ids.isEmpty()) return;
        MapSqlParameterSource params = contextParams(tenantId, clientType).addValue("ids", ids);
        Integer count = namedJdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + table
                        + " WHERE tenant_id = :tenantId AND client_type = :clientType AND id IN (:ids)",
                params, Integer.class);
        if (count == null || count != ids.size()) throw new IllegalArgumentException(label + "不属于当前租户");
    }

    private void validateOrganizationId(ClientSchema schema, Long organizationId, Long tenantId) {
        if (organizationId == null) return;
        Integer count = schema.merchant()
                ? jdbcTemplate.queryForObject("""
                        SELECT COUNT(*) FROM sys_merchant_organization
                        WHERE id = ? AND tenant_id = ? AND status = 1 AND deleted = 0
                        """, Integer.class, organizationId, tenantId)
                : jdbcTemplate.queryForObject("""
                        SELECT COUNT(*) FROM sys_platform_organization
                        WHERE id = ? AND status = 1 AND deleted = 0
                        """, Integer.class, organizationId);
        if (count == null || count == 0) throw new IllegalArgumentException("所属组织不存在或已停用");
    }

    private void validateStoreIds(List<Long> ids, Long tenantId) {
        if (ids.isEmpty()) return;
        MapSqlParameterSource params = new MapSqlParameterSource("tenantId", tenantId).addValue("ids", ids);
        Integer count = namedJdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_store WHERE tenant_id = :tenantId AND status = 1 AND id IN (:ids)",
                params, Integer.class);
        if (count == null || count != ids.size()) throw new IllegalArgumentException("门店不属于当前租户或已停用");
    }

    private void replaceUserRoles(ClientSchema schema, Long userId, List<Long> roleIds) {
        jdbcTemplate.update("DELETE FROM " + schema.userRoleTable()
                + " WHERE " + schema.userIdColumn() + " = ?", userId);
        roleIds.forEach(roleId -> jdbcTemplate.update(
                "INSERT INTO " + schema.userRoleTable()
                        + " (" + schema.userIdColumn() + ", role_id) VALUES (?, ?)", userId, roleId));
    }

    private void replaceUserStores(ClientSchema schema, Long userId, List<Long> storeIds) {
        if (!schema.merchant()) return;
        jdbcTemplate.update("DELETE FROM sys_merchant_user_store WHERE merchant_user_id = ?", userId);
        storeIds.forEach(storeId -> jdbcTemplate.update(
                "INSERT INTO sys_merchant_user_store (merchant_user_id, store_id) VALUES (?, ?)", userId, storeId));
    }

    private void replaceRoleMenus(Long roleId, List<Long> menuIds) {
        jdbcTemplate.update("DELETE FROM sys_role_menu WHERE role_id = ?", roleId);
        menuIds.forEach(menuId -> jdbcTemplate.update(
                "INSERT INTO sys_role_menu (role_id, menu_id) VALUES (?, ?)", roleId, menuId));
    }

    private void assertUserExists(Long id, Long tenantId, String clientType) {
        ClientSchema schema = clientSchema(clientType);
        Integer count = schema.merchant()
                ? jdbcTemplate.queryForObject("""
                        SELECT COUNT(*) FROM sys_merchant_user
                        WHERE id = ? AND tenant_id = ? AND deleted = 0
                        """, Integer.class, id, tenantId)
                : jdbcTemplate.queryForObject("""
                        SELECT COUNT(*) FROM sys_platform_user WHERE id = ? AND deleted = 0
                        """, Integer.class, id);
        if (count == null || count == 0) throw new IllegalArgumentException("用户不存在");
    }

    private boolean userHasPassword(ClientSchema schema, Long id, Long tenantId) {
        Integer count = schema.merchant()
                ? jdbcTemplate.queryForObject("""
                        SELECT COUNT(*) FROM sys_merchant_user
                        WHERE id = ? AND tenant_id = ? AND password_hash IS NOT NULL
                        """, Integer.class, id, tenantId)
                : jdbcTemplate.queryForObject("""
                        SELECT COUNT(*) FROM sys_platform_user WHERE id = ? AND password_hash IS NOT NULL
                        """, Integer.class, id);
        return count != null && count > 0;
    }

    private void assertRoleExists(Long id, Long tenantId, String clientType) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_role WHERE id = ? AND tenant_id = ? AND client_type = ?",
                Integer.class, id, tenantId, normalizeClient(clientType));
        if (count == null || count == 0) throw new IllegalArgumentException("角色不存在");
    }

    private static MapSqlParameterSource contextParams(Long tenantId, String clientType) {
        return new MapSqlParameterSource()
                .addValue("tenantId", tenantId)
                .addValue("clientType", normalizeClient(clientType));
    }

    private static List<Long> distinctIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return new ArrayList<>();
        return new ArrayList<>(new HashSet<>(ids.stream().filter(id -> id != null && id > 0).toList()));
    }

    private static List<String> splitNames(String value) {
        if (value == null || value.isBlank()) return Collections.emptyList();
        return Arrays.asList(value.split(","));
    }

    private static ClientSchema clientSchema(String clientType) {
        String client = normalizeClient(clientType);
        return "MERCHANT".equals(client)
                ? new ClientSchema(true, "sys_merchant_user", "sys_merchant_user_role", "merchant_user_id")
                : new ClientSchema(false, "sys_platform_user", "sys_platform_user_role", "platform_user_id");
    }

    private static String normalizeClient(String clientType) {
        String value = clientType == null ? "" : clientType.trim().toUpperCase(Locale.ROOT);
        if (!Set.of("PLATFORM", "MERCHANT").contains(value)) {
            throw new IllegalArgumentException("客户端类型不正确");
        }
        return value;
    }

    private static String permissionPrefix(String clientType) {
        return normalizeClient(clientType).toLowerCase(Locale.ROOT);
    }

    private static String normalizeDataScope(String dataScope) {
        String value = dataScope == null ? "SELF" : dataScope.trim().toUpperCase(Locale.ROOT);
        if (!DATA_SCOPES.contains(value)) throw new IllegalArgumentException("数据权限类型不正确");
        return value;
    }

    private static int normalizeStatus(Integer status) {
        int value = status == null ? 1 : status;
        if (value != 0 && value != 1) throw new IllegalArgumentException("状态不正确");
        return value;
    }

    private static void validatePassword(String password) {
        if (password == null || password.length() < 8 || password.length() > 64) {
            throw new IllegalArgumentException("密码长度必须为 8-64 位");
        }
    }

    private static String requireText(String value, String label, int maxLength) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(label + "不能为空");
        String result = value.trim();
        if (result.length() > maxLength) throw new IllegalArgumentException(label + "长度不能超过 " + maxLength + " 位");
        return result;
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private record ClientSchema(
            boolean merchant, String userTable, String userRoleTable, String userIdColumn) {
    }

    private record UserInput(
            String username, String displayName, String email, String phone, boolean loginEnabled,
            int status, String dataScope, Long organizationId, Long primaryStoreId,
            List<Long> roleIds, List<Long> storeIds) {
    }

    private record RoleInput(
            String code, String name, int status, String remark, List<Long> menuIds) {
    }
}
