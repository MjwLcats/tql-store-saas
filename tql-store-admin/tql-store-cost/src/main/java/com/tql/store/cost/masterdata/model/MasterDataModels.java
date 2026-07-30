package com.tql.store.cost.masterdata.model;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public final class MasterDataModels {
    private MasterDataModels() {
    }

    public record UnitView(Long id, String unitCode, String unitName, Integer decimalScale, Integer status) {
    }

    public record CreateUnitRequest(
            @NotBlank @Size(max = 64) String unitCode,
            @NotBlank @Size(max = 64) String unitName,
            @NotNull @Min(0) @Max(10) Integer decimalScale) {
    }

    public record MaterialView(
            Long id, String materialCode, String materialName, String specification,
            Long baseUnitId, String externalMaterialCode, String sourceSystem, Integer status) {
    }

    public record CreateMaterialRequest(
            @NotBlank @Size(max = 64) String materialCode,
            @NotBlank @Size(max = 200) String materialName,
            @Size(max = 200) String specification,
            @NotNull Long baseUnitId,
            @Size(max = 128) String externalMaterialCode,
            @NotBlank @Size(max = 32) String sourceSystem) {
    }

    public record DishView(
            Long id, String dishCode, String dishName,
            String externalDishCode, String sourceSystem, Integer status) {
    }

    public record CreateDishRequest(
            @NotBlank @Size(max = 64) String dishCode,
            @NotBlank @Size(max = 200) String dishName,
            @Size(max = 128) String externalDishCode,
            @NotBlank @Size(max = 32) String sourceSystem) {
    }

    public record ConversionView(
            Long id, Long materialId, Long sourceUnitId, Long targetUnitId, BigDecimal conversionRate) {
    }

    public record CreateConversionRequest(
            @NotNull Long materialId,
            @NotNull Long sourceUnitId,
            @NotNull Long targetUnitId,
            @NotNull @DecimalMin(value = "0", inclusive = false) BigDecimal conversionRate) {
    }

    public record PriceView(
            Long id, Long storeId, Long materialId, String priceType, BigDecimal unitPrice,
            LocalDateTime effectiveFrom, LocalDateTime effectiveTo, String sourceSystem) {
    }

    public record CreatePriceRequest(
            @NotNull Long storeId,
            @NotNull Long materialId,
            @NotBlank @Pattern(regexp = "STANDARD|PURCHASE|MANUAL") String priceType,
            @NotNull @DecimalMin("0") BigDecimal unitPrice,
            @NotNull LocalDateTime effectiveFrom,
            LocalDateTime effectiveTo,
            @NotBlank @Size(max = 32) String sourceSystem) {
    }
}
