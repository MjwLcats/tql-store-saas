package com.tql.store.cost.inventory.service;

import com.tql.store.cost.inventory.model.CreateInventoryTaskRequest;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class InventoryTaskServiceTest {

    @Test
    void rejectsAnInvalidTimeWindowBeforeAccessingTheDatabase() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        InventoryTaskService service = new InventoryTaskService(jdbcTemplate);
        LocalDateTime start = LocalDateTime.of(2026, 7, 29, 10, 0);
        CreateInventoryTaskRequest request =
                new CreateInventoryTaskRequest(1L, "月末盘点", start, start, null);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.create(10L, 20L, request));

        assertEquals("计划结束时间必须晚于开始时间", exception.getMessage());
        verifyNoInteractions(jdbcTemplate);
    }
}
