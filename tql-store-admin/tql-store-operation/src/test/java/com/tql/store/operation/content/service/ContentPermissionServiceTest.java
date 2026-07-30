package com.tql.store.operation.content.service;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ContentPermissionServiceTest {

    @Test
    void allowsMerchantUserWithPermission() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForObject(
                anyString(), eq(Integer.class), any(), any(), any(), any(), any()))
                .thenReturn(1);
        ContentPermissionService service = new ContentPermissionService(jdbcTemplate);

        assertDoesNotThrow(() -> service.require(
                9L, 10001L, "MERCHANT", "merchant:content:plan:create"));
    }

    @Test
    void rejectsUserWithoutPermission() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForObject(
                anyString(), eq(Integer.class), any(), any(), any(), any(), any()))
                .thenReturn(0);
        ContentPermissionService service = new ContentPermissionService(jdbcTemplate);

        assertThrows(SecurityException.class, () -> service.require(
                9L, 10001L, "MERCHANT", "merchant:content:plan:publish"));
    }

    @Test
    void rejectsPlatformClientBeforeDatabaseLookup() {
        ContentPermissionService service =
                new ContentPermissionService(mock(JdbcTemplate.class));

        assertThrows(SecurityException.class, () -> service.require(
                1L, 0L, "PLATFORM", "merchant:content:plan:create"));
    }
}
