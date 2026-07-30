package com.tql.store.cost.bom.service;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class BomCostCalculator {

    public CostResult calculate(BigDecimal quantity, BigDecimal conversionRate, BigDecimal unitPrice) {
        if (quantity == null || quantity.signum() <= 0) {
            throw new IllegalArgumentException("BOM物料用量必须大于0");
        }
        if (conversionRate == null || conversionRate.signum() <= 0) {
            throw new IllegalArgumentException("物料单位换算系数必须大于0");
        }
        if (unitPrice == null || unitPrice.signum() < 0) {
            throw new IllegalArgumentException("物料价格不能小于0");
        }
        BigDecimal convertedQuantity =
                quantity.multiply(conversionRate).setScale(6, RoundingMode.HALF_UP);
        BigDecimal itemCost =
                convertedQuantity.multiply(unitPrice).setScale(4, RoundingMode.HALF_UP);
        return new CostResult(convertedQuantity, unitPrice.setScale(6, RoundingMode.HALF_UP), itemCost);
    }

    public record CostResult(
            BigDecimal convertedQuantity,
            BigDecimal unitPrice,
            BigDecimal itemCost) {
    }
}
