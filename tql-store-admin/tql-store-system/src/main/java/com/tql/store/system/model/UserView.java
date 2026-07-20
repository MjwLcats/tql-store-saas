package com.tql.store.system.model;

import java.util.List;

public record UserView(
        Long id,
        String username,
        String employeeNumber,
        String displayName,
        String organizationName,
        String email,
        String phone,
        boolean loginEnabled,
        String sourceType,
        int status,
        String dataScope,
        Long primaryStoreId,
        String primaryStoreName,
        List<String> roleNames
) {
}
