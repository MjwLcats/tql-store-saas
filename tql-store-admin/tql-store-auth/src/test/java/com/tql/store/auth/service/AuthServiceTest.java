package com.tql.store.auth.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tql.store.auth.model.ChangePasswordRequest;
import com.tql.store.common.security.PasswordHasher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private StringRedisTemplate redisTemplate;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(jdbcTemplate, redisTemplate, new ObjectMapper());
    }

    @Test
    void rejectsWrongCurrentPassword() {
        String currentHash = PasswordHasher.encode("currentPassword123");
        when(jdbcTemplate.queryForObject(anyString(), eq(String.class), eq(9L), eq(3L)))
                .thenReturn(currentHash);

        assertThrows(IllegalArgumentException.class, () -> authService.changePassword(
                9L, 3L, "MERCHANT", "Bearer session-token",
                new ChangePasswordRequest("wrongPassword", "newPassword456")));

        verify(jdbcTemplate, never()).update(anyString(), any(), any(), any());
    }

    @Test
    void changesMerchantPasswordWithinTenantAndRevokesCurrentSession() {
        String currentHash = PasswordHasher.encode("currentPassword123");
        when(jdbcTemplate.queryForObject(anyString(), eq(String.class), eq(9L), eq(3L)))
                .thenReturn(currentHash);
        when(jdbcTemplate.update(anyString(), any(), any(), any())).thenReturn(1);

        authService.changePassword(
                9L, 3L, "MERCHANT", "Bearer session-token",
                new ChangePasswordRequest("currentPassword123", "newPassword456"));

        verify(jdbcTemplate).update(
                argThat(sql -> sql.contains("sys_merchant_user") && sql.contains("tenant_id = ?")),
                argThat((String hash) -> PasswordHasher.matches("newPassword456", hash)),
                eq(9L),
                eq(3L));
        verify(redisTemplate).delete(AuthService.SESSION_PREFIX + "session-token");
    }
}
