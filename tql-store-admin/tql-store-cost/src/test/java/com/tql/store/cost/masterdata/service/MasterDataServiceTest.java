package com.tql.store.cost.masterdata.service;

import com.tql.store.cost.masterdata.model.MasterDataModels.CreatePriceRequest;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class MasterDataServiceTest {

    @Test
    void rejectsAnInvalidPricePeriodBeforeAccessingTheDatabase() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        MasterDataService service = new MasterDataService(jdbcTemplate);
        LocalDateTime effectiveFrom = LocalDateTime.of(2026, 7, 29, 10, 0);
        CreatePriceRequest request = new CreatePriceRequest(
                1L, 2L, "STANDARD", new BigDecimal("3.5"),
                effectiveFrom, effectiveFrom, "SAAS");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.createPrice(10L, 20L, request));

        assertEquals("价格失效时间必须晚于生效时间", exception.getMessage());
        verifyNoInteractions(jdbcTemplate);
    }
}
