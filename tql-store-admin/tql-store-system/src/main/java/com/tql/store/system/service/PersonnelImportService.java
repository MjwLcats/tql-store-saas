package com.tql.store.system.service;

import com.tql.store.system.model.PersonnelImportResult;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class PersonnelImportService {
    private final JdbcTemplate jdbcTemplate;

    public PersonnelImportService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public byte[] template() {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("导入人员");
            Row header = sheet.createRow(0);
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFillForegroundColor(IndexedColors.ROYAL_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            Font font = workbook.createFont();
            font.setBold(true);
            font.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(font);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            String[] headers = {"姓名", "手机号"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, i == 0 ? 18 * 256 : 24 * 256);
            }
            sheet.createFreezePane(0, 1);
            workbook.write(output);
            return output.toByteArray();
        } catch (Exception exception) {
            throw new IllegalStateException("人员导入模板生成失败", exception);
        }
    }

    public List<PersonnelImportResult> validate(Long tenantId, MultipartFile file) {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("请选择Excel文件");
        if (file.getOriginalFilename() == null || !file.getOriginalFilename().toLowerCase().endsWith(".xlsx")) {
            throw new IllegalArgumentException("仅支持.xlsx格式文件");
        }
        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null || sheet.getPhysicalNumberOfRows() == 0) {
                throw new IllegalArgumentException("Excel内容为空");
            }
            DataFormatter formatter = new DataFormatter();
            Row header = sheet.getRow(0);
            if (header == null
                    || !"姓名".equals(formatter.formatCellValue(header.getCell(0)).trim())
                    || !"手机号".equals(formatter.formatCellValue(header.getCell(1)).trim())) {
                throw new IllegalArgumentException("模板表头必须为：姓名、手机号");
            }
            List<PersonnelImportResult> results = new ArrayList<>();
            Set<String> imported = new HashSet<>();
            for (int index = 1; index <= sheet.getLastRowNum(); index++) {
                Row row = sheet.getRow(index);
                if (row == null) continue;
                String name = formatter.formatCellValue(row.getCell(0)).trim();
                String phone = normalizePhone(formatter.formatCellValue(row.getCell(1)));
                if (name.isBlank() && phone.isBlank()) continue;
                if (results.size() >= 1000) throw new IllegalArgumentException("单次最多导入1000人");
                results.add(validateRow(tenantId, index + 1, name, phone, imported));
            }
            if (results.isEmpty()) throw new IllegalArgumentException("Excel中没有可校验的人员");
            return results;
        } catch (IllegalArgumentException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalArgumentException("Excel解析失败，请使用系统模板填写", exception);
        }
    }

    private PersonnelImportResult validateRow(
            Long tenantId, int rowNumber, String inputName, String inputPhone, Set<String> imported) {
        if (inputName.isBlank() || !inputPhone.matches("^1\\d{10}$")) {
            return invalid(rowNumber, inputName, inputPhone, "INVALID", "姓名不能为空，手机号必须为11位");
        }
        String key = inputName + "|" + inputPhone;
        if (!imported.add(key)) {
            return invalid(rowNumber, inputName, inputPhone, "DUPLICATE", "Excel中存在重复人员");
        }

        List<PersonnelImportResult> exact = jdbcTemplate.query("""
                SELECT u.id,
                       COALESCE(store.org_name, '-') AS organization_store,
                       u.display_name,
                       u.phone,
                       COALESCE(organization.org_name, '-') AS department,
                       COALESCE(u.post_name, u.position_name, '-') AS position_name
                FROM sys_merchant_user u
                LEFT JOIN sys_merchant_organization organization
                  ON organization.id = u.organization_id AND organization.tenant_id = u.tenant_id
                LEFT JOIN sys_merchant_organization store
                  ON store.id = u.primary_store_id
                 AND store.tenant_id = u.tenant_id
                 AND store.org_type = 'STORE'
                WHERE u.tenant_id = ?
                  AND TRIM(u.display_name) = ?
                  AND REPLACE(REPLACE(TRIM(u.phone), ' ', ''), '-', '') = ?
                  AND u.deleted = 0
                """, (rs, rowNum) -> new PersonnelImportResult(
                rowNumber, inputName, inputPhone, rs.getLong("id"),
                rs.getString("organization_store"), rs.getString("display_name"), rs.getString("phone"),
                rs.getString("department"), rs.getString("position_name"), "VALID", "校验通过"
        ), tenantId, inputName, inputPhone);
        if (exact.size() == 1) return exact.get(0);
        if (exact.size() > 1) {
            return invalid(rowNumber, inputName, inputPhone, "DUPLICATE_USER", "用户表存在重复姓名和手机号");
        }

        Integer related = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM sys_merchant_user
                WHERE tenant_id = ?
                  AND (TRIM(display_name) = ?
                       OR REPLACE(REPLACE(TRIM(phone), ' ', ''), '-', '') = ?)
                  AND deleted = 0
                """, Integer.class, tenantId, inputName, inputPhone);
        return invalid(rowNumber, inputName, inputPhone,
                related != null && related > 0 ? "MISMATCH" : "NOT_FOUND",
                related != null && related > 0 ? "姓名与手机号不一致" : "用户表中不存在该人员");
    }

    private String normalizePhone(String phone) {
        if (phone == null) return "";
        return phone.trim()
                .replaceAll("[\\s-]+", "")
                .replaceFirst("^\\+?86", "");
    }

    private PersonnelImportResult invalid(
            int rowNumber, String name, String phone, String status, String message) {
        return new PersonnelImportResult(rowNumber, name, phone, null, "-", name, phone, "-", "-", status, message);
    }
}
