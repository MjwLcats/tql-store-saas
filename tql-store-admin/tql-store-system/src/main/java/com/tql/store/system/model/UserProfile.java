package com.tql.store.system.model;

public record UserProfile(
        Long id,
        Long tenantId,
        String tenantName,
        Long primaryStoreId,
        String primaryStoreName,
        String dataScope,
        String username,
        String displayName,
        String email,
        String phone,
        String clientType
) {
}
