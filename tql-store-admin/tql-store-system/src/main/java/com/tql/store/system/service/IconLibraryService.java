package com.tql.store.system.service;

import com.tql.store.system.model.IconUpdateRequest;
import com.tql.store.system.model.IconView;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
public class IconLibraryService {
    private static final Pattern CODE = Pattern.compile("[A-Za-z][A-Za-z0-9_-]{1,63}");
    private static final Pattern UNSAFE = Pattern.compile(
            "(?is)<\\s*(script|foreignObject|iframe|object|embed|style|image)\\b|\\bon\\w+\\s*=|(?:href|src)\\s*=\\s*['\"]\\s*(?:https?:|data:|javascript:)");
    private final JdbcTemplate jdbcTemplate;

    public IconLibraryService(JdbcTemplate jdbcTemplate) { this.jdbcTemplate = jdbcTemplate; }

    public List<IconView> list(String keyword, String category, Integer status) {
        String term = keyword == null ? "" : keyword.trim();
        return jdbcTemplate.query("""
            SELECT i.id,i.icon_name,i.icon_code,i.category,i.source_type,i.svg_content,
                   i.status,i.sort_order,COUNT(m.id) usage_count
            FROM sys_icon i LEFT JOIN sys_menu m ON m.icon_id=i.id AND m.deleted=0
            WHERE (?='' OR i.icon_name LIKE CONCAT('%',?,'%') OR i.icon_code LIKE CONCAT('%',?,'%'))
              AND (? IS NULL OR i.category=?) AND (? IS NULL OR i.status=?)
            GROUP BY i.id ORDER BY i.sort_order,i.id
            """, (rs,n) -> new IconView(rs.getLong("id"),rs.getString("icon_name"),
                rs.getString("icon_code"),rs.getString("category"),rs.getString("source_type"),
                rs.getString("svg_content"),rs.getInt("status"),rs.getInt("sort_order"),
                rs.getInt("usage_count")), term,term,term,category,category,status,status);
    }

    @Transactional
    public Long upload(String name, String code, String category, Integer order,
                       MultipartFile file, Long operatorId) {
        String safeName = text(name,"图标名称",64);
        String safeCode = text(code,"图标编码",64);
        if (!CODE.matcher(safeCode).matches()) throw new IllegalArgumentException("图标编码格式不正确");
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("请选择SVG文件");
        if (file.getSize() > 50 * 1024) throw new IllegalArgumentException("SVG文件不能超过50KB");
        String original = file.getOriginalFilename();
        if (original == null || !original.toLowerCase(Locale.ROOT).endsWith(".svg"))
            throw new IllegalArgumentException("只允许上传SVG文件");
        String svg;
        try { svg = new String(file.getBytes(), StandardCharsets.UTF_8).trim(); }
        catch (Exception e) { throw new IllegalArgumentException("SVG文件读取失败"); }
        if (!svg.matches("(?is)^<svg\\b[\\s\\S]*</svg>$") || !svg.matches("(?is)[\\s\\S]*\\bviewBox\\s*=\\s*['\"][^'\"]+['\"][\\s\\S]*"))
            throw new IllegalArgumentException("SVG必须包含svg根节点和viewBox");
        if (UNSAFE.matcher(svg).find() || svg.contains("<!DOCTYPE") || svg.contains("<?xml"))
            throw new IllegalArgumentException("SVG包含不安全内容");
        Integer exists = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM sys_icon WHERE icon_code=?",Integer.class,safeCode);
        if (exists != null && exists > 0) throw new IllegalArgumentException("图标编码已存在");
        jdbcTemplate.update("""
            INSERT INTO sys_icon(icon_name,icon_code,category,source_type,svg_content,status,sort_order,created_by)
            VALUES(?,?,?,'CUSTOM',?,1,?,?)
            """,safeName,safeCode,text(category,"分类",32),svg,order==null?0:order,operatorId);
        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()",Long.class);
    }

    public void update(Long id, IconUpdateRequest request) {
        get(id);
        jdbcTemplate.update("UPDATE sys_icon SET icon_name=?,category=?,sort_order=? WHERE id=?",
                text(request.name(),"图标名称",64),text(request.category(),"分类",32),
                request.order()==null?0:request.order(),id);
    }
    public void status(Long id, Integer status) {
        get(id);
        if (status == null || (status != 0 && status != 1)) throw new IllegalArgumentException("状态不正确");
        jdbcTemplate.update("UPDATE sys_icon SET status=? WHERE id=?",status,id);
    }
    @Transactional public void delete(Long id) {
        IconView icon=get(id);
        if ("SYSTEM".equals(icon.sourceType())) throw new IllegalArgumentException("系统内置图标不允许删除");
        if (icon.usageCount()>0) throw new IllegalArgumentException("图标正在被菜单使用，不能删除");
        jdbcTemplate.update("DELETE FROM sys_icon WHERE id=?",id);
    }
    public IconView get(Long id) {
        return list(null,null,null).stream().filter(i->i.id().equals(id)).findFirst()
                .orElseThrow(()->new IllegalArgumentException("图标不存在"));
    }
    private String text(String value,String label,int max) {
        if(value==null||value.isBlank()) throw new IllegalArgumentException(label+"不能为空");
        String v=value.trim(); if(v.length()>max) throw new IllegalArgumentException(label+"长度不能超过"+max+"位"); return v;
    }
}
