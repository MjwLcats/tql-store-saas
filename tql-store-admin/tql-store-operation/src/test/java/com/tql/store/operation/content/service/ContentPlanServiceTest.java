package com.tql.store.operation.content.service;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ContentPlanServiceTest {

    @Test
    void terminateActivityPersistsActivityPlansAndUnfinishedTasks() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.query(anyString(), any(org.springframework.jdbc.core.RowMapper.class), any(), any()))
                .thenReturn(List.of("ACTIVE"));
        ContentPlanService service = new ContentPlanService(
                jdbcTemplate, mock(NamedParameterJdbcTemplate.class));

        service.terminateActivity(10001L, 9L, 5L);

        verify(jdbcTemplate, times(3)).update(anyString(), any(), any(), any());
    }

    @Test
    void terminateActivityIsIdempotentWhenAlreadyTerminated() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.query(anyString(), any(org.springframework.jdbc.core.RowMapper.class), any(), any()))
                .thenReturn(List.of("TERMINATED"));
        ContentPlanService service = new ContentPlanService(
                jdbcTemplate, mock(NamedParameterJdbcTemplate.class));

        service.terminateActivity(10001L, 9L, 5L);

        verify(jdbcTemplate, never()).update(anyString(), any(), any(), any());
    }
}
