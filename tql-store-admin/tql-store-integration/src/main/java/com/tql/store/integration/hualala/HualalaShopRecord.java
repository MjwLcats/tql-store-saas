package com.tql.store.integration.hualala;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record HualalaShopRecord(
        String externalShopId,
        String externalGroupId,
        String externalShopCode,
        String shopName,
        String brandId,
        String brandName,
        String businessModel,
        String operationMode,
        String businessStatus,
        String cityCode,
        String cityName,
        String address,
        String shopPhone,
        String shopOpenTime,
        String imagePath,
        BigDecimal longitude,
        BigDecimal latitude,
        String recordAction,
        LocalDateTime sourceCreateTime,
        LocalDateTime sourceUpdateTime,
        String rawJson
) {
}
