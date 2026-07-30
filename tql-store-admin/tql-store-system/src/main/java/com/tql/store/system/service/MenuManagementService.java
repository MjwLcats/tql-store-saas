package com.tql.store.system.service;

import com.tql.store.system.model.MerchantMenuSaveRequest;
import com.tql.store.system.model.MerchantMenuView;
import com.tql.store.system.model.MerchantOption;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class MenuManagementService {

    private static final Set<String> TYPES = Set.of("DIRECTORY", "MENU", "BUTTON");
    private static final Set<String> COMPONENTS = Set.of(
            "dashboard", "content", "users", "roles", "integration-sync", "profile");

    private final JdbcTemplate jdbcTemplate;

    public MenuManagementService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<MerchantOption> listMerchants() {
        return jdbcTemplate.query("""
                SELECT id, tenant_code, tenant_name
                FROM sys_tenant
                WHERE status = 1
                ORDER BY id
                """, (rs, rowNum) -> new MerchantOption(
                rs.getLong("id"), rs.getString("tenant_code"), rs.getString("tenant_name")));
    }

    public List<MerchantMenuView> list(Long tenantId) {
        assertTenant(tenantId);
        return jdbcTemplate.query("""
                SELECT m.id, m.tenant_id, m.parent_id, m.menu_name, m.menu_type, m.route_name, m.route_path,
                       m.component_key, m.icon, m.icon_id, i.svg_content AS icon_svg,
                       m.permission_code, m.sort_order, m.visible, m.status, m.system_builtin
                FROM sys_menu m LEFT JOIN sys_icon i ON i.id=m.icon_id
                WHERE m.tenant_id = ? AND m.client_type = 'MERCHANT' AND m.deleted = 0
                ORDER BY m.sort_order, m.id
                """, (rs, rowNum) -> map(rs), tenantId);
    }

    public MerchantMenuView get(Long id, Long tenantId) {
        List<MerchantMenuView> records = jdbcTemplate.query("""
                SELECT m.id, m.tenant_id, m.parent_id, m.menu_name, m.menu_type, m.route_name, m.route_path,
                       m.component_key, m.icon, m.icon_id, i.svg_content AS icon_svg,
                       m.permission_code, m.sort_order, m.visible, m.status, m.system_builtin
                FROM sys_menu m LEFT JOIN sys_icon i ON i.id=m.icon_id
                WHERE m.id = ? AND m.tenant_id = ? AND m.client_type = 'MERCHANT' AND m.deleted = 0
                """, (rs, rowNum) -> map(rs), id, tenantId);
        if (records.isEmpty()) throw new IllegalArgumentException("菜单不存在");
        return records.get(0);
    }

    @Transactional
    public Long create(MerchantMenuSaveRequest request) {
        MenuInput input = validate(request, null);
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO sys_menu
                        (tenant_id, parent_id, menu_name, menu_type, route_name, route_path,
                         component_key, icon, icon_id, permission_code, client_type, sort_order,
                         visible, status, system_builtin, deleted)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'MERCHANT', ?, ?, ?, 0, 0)
                    """, Statement.RETURN_GENERATED_KEYS);
            statement.setLong(1, input.tenantId());
            statement.setLong(2, input.parentId());
            statement.setString(3, input.name());
            statement.setString(4, input.type());
            statement.setString(5, input.routeName());
            statement.setString(6, input.path());
            statement.setString(7, input.componentKey());
            statement.setString(8, input.icon());
            if (input.iconId() == null) statement.setNull(9, java.sql.Types.BIGINT); else statement.setLong(9, input.iconId());
            statement.setString(10, input.permission());
            statement.setInt(11, input.order());
            statement.setInt(12, input.visible());
            statement.setInt(13, input.status());
            return statement;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key == null) throw new IllegalStateException("菜单创建失败");
        jdbcTemplate.update("""
                INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
                SELECT id, ?
                FROM sys_role
                WHERE tenant_id = ? AND client_type = 'MERCHANT'
                  AND role_code = 'MERCHANT_ADMIN' AND status = 1
                """, key.longValue(), input.tenantId());
        return key.longValue();
    }

    @Transactional
    public void update(Long id, MerchantMenuSaveRequest request) {
        MerchantMenuView current = get(id, request.tenantId());
        MenuInput input = validate(request, id);
        if (current.systemBuiltin()) throw new IllegalArgumentException("系统内置菜单不允许修改");
        if (isDescendant(id, input.parentId(), input.tenantId())) {
            throw new IllegalArgumentException("上级菜单不能选择当前菜单或其子级");
        }
        jdbcTemplate.update("""
                UPDATE sys_menu
                SET parent_id = ?, menu_name = ?, menu_type = ?, route_name = ?, route_path = ?,
                    component_key = ?, icon = ?, icon_id = ?, permission_code = ?, sort_order = ?,
                    visible = ?, status = ?
                WHERE id = ? AND tenant_id = ? AND client_type = 'MERCHANT' AND deleted = 0
                """, input.parentId(), input.name(), input.type(), input.routeName(), input.path(),
                input.componentKey(), input.icon(), input.iconId(), input.permission(), input.order(),
                input.visible(), input.status(), id, input.tenantId());
    }

    public void updateStatus(Long id, Long tenantId, Integer status) {
        normalizeFlag(status, "状态");
        MerchantMenuView current = get(id, tenantId);
        if (current.systemBuiltin()) throw new IllegalArgumentException("系统内置菜单不允许停用");
        jdbcTemplate.update("UPDATE sys_menu SET status = ? WHERE id = ? AND tenant_id = ?",
                status, id, tenantId);
    }

    public void updateVisibility(Long id, Long tenantId, Integer visible) {
        normalizeFlag(visible, "显示状态");
        MerchantMenuView current = get(id, tenantId);
        if ("BUTTON".equals(current.type())) throw new IllegalArgumentException("按钮无需设置显示状态");
        jdbcTemplate.update("UPDATE sys_menu SET visible = ? WHERE id = ? AND tenant_id = ?",
                visible, id, tenantId);
    }

    @Transactional
    public void delete(Long id, Long tenantId) {
        MerchantMenuView current = get(id, tenantId);
        if (current.systemBuiltin()) throw new IllegalArgumentException("系统内置菜单不允许删除");
        Integer childCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM sys_menu
                WHERE parent_id = ? AND tenant_id = ? AND client_type = 'MERCHANT' AND deleted = 0
                """, Integer.class, id, tenantId);
        if (childCount != null && childCount > 0) {
            throw new IllegalArgumentException("该节点存在子级，请先删除或移动子级");
        }
        jdbcTemplate.update("DELETE FROM sys_role_menu WHERE menu_id = ?", id);
        jdbcTemplate.update("""
                UPDATE sys_menu SET deleted = 1
                WHERE id = ? AND tenant_id = ? AND client_type = 'MERCHANT'
                """, id, tenantId);
    }

    private MenuInput validate(MerchantMenuSaveRequest request, Long currentId) {
        if (request == null) throw new IllegalArgumentException("菜单参数不能为空");
        Long tenantId = request.tenantId();
        assertTenant(tenantId);
        long parentId = request.parentId() == null ? 0 : request.parentId();
        String type = text(request.type(), "节点类型", 16).toUpperCase(Locale.ROOT);
        if (!TYPES.contains(type)) throw new IllegalArgumentException("节点类型不正确");
        String name = text(request.name(), "菜单名称", 64);

        MerchantMenuView parent = null;
        if (parentId != 0) parent = get(parentId, tenantId);
        if ("BUTTON".equals(type) && (parent == null || !"MENU".equals(parent.type()))) {
            throw new IllegalArgumentException("按钮只能创建在菜单下");
        }
        if ("MENU".equals(type) && parent != null && !"DIRECTORY".equals(parent.type())) {
            throw new IllegalArgumentException("菜单只能创建在目录下");
        }
        if ("DIRECTORY".equals(type) && parent != null && !"DIRECTORY".equals(parent.type())) {
            throw new IllegalArgumentException("目录只能创建在目录下");
        }

        String routeName = null;
        String path = null;
        String componentKey = null;
        String permission = trim(request.permission());
        String icon = trim(request.icon());
        Long iconId = request.iconId();
        if ("DIRECTORY".equals(type)) {
            path = optionalPath(request.path());
            permission = null;
        } else if ("MENU".equals(type)) {
            routeName = text(request.routeName(), "路由名称", 64);
            path = path(request.path());
            componentKey = text(request.componentKey(), "组件标识", 64);
            if (!COMPONENTS.contains(componentKey)) throw new IllegalArgumentException("组件标识未注册");
            permission = text(permission, "权限编码", 128);
        } else {
            permission = text(permission, "权限编码", 128);
            icon = null;
            iconId = null;
        }
        if (iconId != null) {
            List<Map<String,Object>> found = jdbcTemplate.queryForList(
                    "SELECT icon_code FROM sys_icon WHERE id=? AND status=1", iconId);
            if (found.isEmpty()) throw new IllegalArgumentException("所选图标不存在或已停用");
            icon = String.valueOf(found.get(0).get("icon_code"));
        }

        assertUnique(tenantId, parentId, name, routeName, path, permission, currentId);
        return new MenuInput(tenantId, parentId, name, type, routeName, path, componentKey,
                icon, iconId, permission, request.order() == null ? 0 : request.order(),
                "BUTTON".equals(type) ? 0 : normalizeFlag(request.visible(), "显示状态"),
                normalizeFlag(request.status(), "状态"));
    }

    private void assertUnique(Long tenantId, long parentId, String name, String routeName,
                              String path, String permission, Long currentId) {
        long excluded = currentId == null ? -1 : currentId;
        Integer sameName = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM sys_menu
                WHERE tenant_id = ? AND client_type = 'MERCHANT' AND parent_id = ?
                  AND menu_name = ? AND id <> ? AND deleted = 0
                """, Integer.class, tenantId, parentId, name, excluded);
        if (sameName != null && sameName > 0) throw new IllegalArgumentException("同级菜单名称已存在");
        if (routeName != null) assertFieldUnique("route_name", routeName, tenantId, excluded, "路由名称已存在");
        if (path != null) assertFieldUnique("route_path", path, tenantId, excluded, "路由地址已存在");
        if (permission != null) assertFieldUnique("permission_code", permission, tenantId, excluded, "权限编码已存在");
    }

    private void assertFieldUnique(String column, String value, Long tenantId, long excluded, String message) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_menu WHERE tenant_id = ? AND client_type = 'MERCHANT' "
                        + "AND " + column + " = ? AND id <> ? AND deleted = 0",
                Integer.class, tenantId, value, excluded);
        if (count != null && count > 0) throw new IllegalArgumentException(message);
    }

    private boolean isDescendant(Long currentId, Long parentId, Long tenantId) {
        if (parentId == null || parentId == 0) return false;
        if (currentId.equals(parentId)) return true;
        Integer count = jdbcTemplate.queryForObject("""
                WITH RECURSIVE descendants AS (
                    SELECT id FROM sys_menu WHERE parent_id = ? AND tenant_id = ? AND deleted = 0
                    UNION ALL
                    SELECT child.id FROM sys_menu child
                    JOIN descendants parent ON child.parent_id = parent.id
                    WHERE child.tenant_id = ? AND child.deleted = 0
                )
                SELECT COUNT(*) FROM descendants WHERE id = ?
                """, Integer.class, currentId, tenantId, tenantId, parentId);
        return count != null && count > 0;
    }

    private void assertTenant(Long tenantId) {
        if (tenantId == null || tenantId <= 0) throw new IllegalArgumentException("请选择商家");
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_tenant WHERE id = ? AND status = 1", Integer.class, tenantId);
        if (count == null || count == 0) throw new IllegalArgumentException("商家不存在或已停用");
    }

    private MerchantMenuView map(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new MerchantMenuView(
                rs.getLong("id"), rs.getLong("tenant_id"), rs.getLong("parent_id"),
                rs.getString("menu_name"), rs.getString("menu_type"),
                rs.getString("route_name"), rs.getString("route_path"),
                rs.getString("component_key"), rs.getString("icon"),
                rs.getObject("icon_id", Long.class), rs.getString("icon_svg"),
                rs.getString("permission_code"), rs.getInt("sort_order"),
                rs.getInt("visible"), rs.getInt("status"),
                rs.getInt("system_builtin") == 1);
    }

    private int normalizeFlag(Integer value, String label) {
        int normalized = value == null ? 1 : value;
        if (normalized != 0 && normalized != 1) throw new IllegalArgumentException(label + "不正确");
        return normalized;
    }

    private String text(String value, String label, int max) {
        String result = trim(value);
        if (result == null) throw new IllegalArgumentException(label + "不能为空");
        if (result.length() > max) throw new IllegalArgumentException(label + "长度不能超过 " + max + " 位");
        return result;
    }

    private String path(String value) {
        String result = text(value, "路由地址", 128);
        if (!result.startsWith("/")) throw new IllegalArgumentException("路由地址必须以 / 开头");
        return result;
    }

    private String optionalPath(String value) {
        String result = trim(value);
        if (result != null && !result.startsWith("/")) throw new IllegalArgumentException("路由地址必须以 / 开头");
        return result;
    }

    private String trim(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private record MenuInput(
            Long tenantId, long parentId, String name, String type, String routeName,
            String path, String componentKey, String icon, Long iconId, String permission,
            int order, int visible, int status) {
    }
}
