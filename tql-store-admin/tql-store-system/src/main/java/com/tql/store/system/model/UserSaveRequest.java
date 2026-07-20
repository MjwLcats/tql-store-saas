package com.tql.store.system.model;

import java.util.List;

public record UserSaveRequest(
        String username,
        String password,
        Boolean loginEnabled,
        Long organizationId,
        String displayName,
        String email,
        String phone,
        Integer status,
        String dataScope,
        Long primaryStoreId,
        List<Long> roleIds,
        List<Long> storeIds
) {
}
