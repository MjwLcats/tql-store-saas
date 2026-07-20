package com.tql.store.common.security;

public record SessionUser(
        Long userId,
        Long tenantId,
        Long primaryStoreId,
        String username,
        String displayName,
        String clientType,
        String dataScope
) {
}
