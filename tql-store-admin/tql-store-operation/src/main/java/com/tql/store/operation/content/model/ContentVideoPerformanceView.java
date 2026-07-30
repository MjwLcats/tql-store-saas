package com.tql.store.operation.content.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ContentVideoPerformanceView(
        Long id,
        Long taskId,
        Long accountId,
        String accountName,
        String platform,
        String platformVideoId,
        String videoTitle,
        String videoUrl,
        LocalDateTime publishTime,
        long viewCount,
        long likeCount,
        long commentCount,
        long shareCount,
        long favoriteCount,
        long followerGain,
        long conversionCount,
        BigDecimal transactionAmount,
        String syncStatus,
        LocalDateTime lastSyncTime
) {
}
