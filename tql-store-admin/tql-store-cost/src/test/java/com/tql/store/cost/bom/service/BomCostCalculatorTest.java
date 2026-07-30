package com.tql.store.cost.bom.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BomCostCalculatorTest {
    private final BomCostCalculator calculator = new BomCostCalculator();

    @Test
    void calculatesConvertedQuantityAndCostWithFinancialPrecision() {
        BomCostCalculator.CostResult result = calculator.calculate(
                new BigDecimal("2.5"),
                new BigDecimal("10"),
                new BigDecimal("3.456789"));

        assertEquals(new BigDecimal("25.000000"), result.convertedQuantity());
        assertEquals(new BigDecimal("3.456789"), result.unitPrice());
        assertEquals(new BigDecimal("86.4197"), result.itemCost());
    }

    @Test
    void rejectsInvalidBusinessInputs() {
        assertThrows(IllegalArgumentException.class, () -> calculator.calculate(
                BigDecimal.ZERO, BigDecimal.ONE, BigDecimal.ONE));
        assertThrows(IllegalArgumentException.class, () -> calculator.calculate(
                BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ONE));
        assertThrows(IllegalArgumentException.class, () -> calculator.calculate(
                BigDecimal.ONE, BigDecimal.ONE, new BigDecimal("-0.01")));
    }
}
