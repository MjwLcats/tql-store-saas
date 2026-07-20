package com.tql.store.system.model;

import java.util.List;

public record UserDetail(
        Long id,
        String username,
        String employeeNumber,
        Boolean loginEnabled,
        String sourceType,
        Long organizationId,
        String organizationName,
        String displayName,
        String email,
        String phone,
        int status,
        String dataScope,
        Long primaryStoreId,
        List<Long> roleIds,
        List<Long> storeIds
) {
}
