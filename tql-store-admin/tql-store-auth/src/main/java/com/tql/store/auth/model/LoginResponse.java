package com.tql.store.auth.model;

public record LoginResponse(
        String token,
        long expiresIn,
        UserSummary user
) {
    public record UserSummary(Long id, Long tenantId, String username, String displayName, String clientType) {
    }
}
