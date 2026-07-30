package com.tql.store.system.model;

public record PersonnelImportResult(
        int rowNumber,
        String inputName,
        String inputPhone,
        Long userId,
        String organizationStore,
        String name,
        String phone,
        String department,
        String position,
        String status,
        String message
) {
}
