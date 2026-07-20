package com.tql.store.auth.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tql.store.auth.model.LoginRequest;
import com.tql.store.auth.model.LoginResponse;
import com.tql.store.common.security.PasswordHasher;
import com.tql.store.common.security.SessionUser;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class AuthService {

    public static final String SESSION_PREFIX = "tql:store:session:";
    private static final Duration SESSION_TTL = Duration.ofHours(8);
    private static final Set<String> SUPPORTED_CLIENTS = Set.of("PLATFORM", "MERCHANT");

    private final JdbcTemplate jdbcTemplate;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public AuthService(JdbcTemplate jdbcTemplate, StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public LoginResponse login(LoginRequest request) {
        String clientType = request.clientType().trim().toUpperCase(Locale.ROOT);
        if (!SUPPORTED_CLIENTS.contains(clientType)) {
            throw new IllegalArgumentException("客户端类型不正确");
        }

        String merchantNo = request.merchantNo() == null ? "" : request.merchantNo().trim();
        if ("MERCHANT".equals(clientType) && merchantNo.isEmpty()) {
            throw new IllegalArgumentException("请输入商户号");
        }

        UserCredential credential;
        try {
            String sql = "MERCHANT".equals(clientType) ? """
                    SELECT u.id, u.tenant_id, u.primary_store_id, u.username, u.password_hash,
                           u.display_name, 'MERCHANT' AS client_type, u.data_scope, u.status
                    FROM sys_merchant_user u
                    INNER JOIN sys_tenant t ON t.id = u.tenant_id
                    WHERE t.tenant_code = ? AND t.status = 1
                      AND u.username = ? AND u.login_enabled = 1 AND u.deleted = 0
                    """ : """
                    SELECT u.id, 0 AS tenant_id, NULL AS primary_store_id,
                           u.username, u.password_hash, u.display_name,
                           'PLATFORM' AS client_type, u.data_scope, u.status
                    FROM sys_platform_user u
                    WHERE u.username = ? AND u.deleted = 0
                    """;
            Object[] params = "MERCHANT".equals(clientType)
                    ? new Object[]{merchantNo, request.username().trim()}
                    : new Object[]{request.username().trim()};
            credential = jdbcTemplate.queryForObject(sql, (rs, rowNum) -> new UserCredential(
                    rs.getLong("id"),
                    rs.getLong("tenant_id"),
                    rs.getObject("primary_store_id", Long.class),
                    rs.getString("username"),
                    rs.getString("password_hash"),
                    rs.getString("display_name"),
                    rs.getString("client_type"),
                    rs.getString("data_scope"),
                    rs.getInt("status")
            ), params);
        } catch (EmptyResultDataAccessException ex) {
            throw invalidCredentials(clientType);
        }

        if (credential == null || credential.status() != 1
                || !PasswordHasher.matches(request.password(), credential.passwordHash())) {
            throw invalidCredentials(clientType);
        }

        try {
            String token = UUID.randomUUID().toString().replace("-", "")
                    + UUID.randomUUID().toString().replace("-", "");
            SessionUser sessionUser = new SessionUser(
                    credential.id(), credential.tenantId(), credential.primaryStoreId(),
                    credential.username(), credential.displayName(), credential.clientType(),
                    credential.dataScope());
            redisTemplate.opsForValue().set(
                    SESSION_PREFIX + token,
                    objectMapper.writeValueAsString(sessionUser),
                    SESSION_TTL
            );
            return new LoginResponse(
                    token,
                    SESSION_TTL.toSeconds(),
                    new LoginResponse.UserSummary(
                            credential.id(), credential.tenantId(), credential.username(),
                            credential.displayName(), credential.clientType())
            );
        } catch (Exception ex) {
            throw new IllegalStateException("登录状态创建失败", ex);
        }
    }

    public void logout(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return;
        }
        String token = authorization.substring(7).trim();
        if (!token.isEmpty()) {
            redisTemplate.delete(SESSION_PREFIX + token);
        }
    }

    private IllegalArgumentException invalidCredentials(String clientType) {
        return new IllegalArgumentException("MERCHANT".equals(clientType)
                ? "商户号、用户名或密码错误"
                : "用户名或密码错误");
    }

    private record UserCredential(
            Long id,
            Long tenantId,
            Long primaryStoreId,
            String username,
            String passwordHash,
            String displayName,
            String clientType,
            String dataScope,
            int status
    ) {
    }
}
