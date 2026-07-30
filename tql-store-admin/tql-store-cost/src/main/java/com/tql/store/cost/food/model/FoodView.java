package com.tql.store.cost.food.model;

public record FoodView(
        Long id, Long foodId, Long shopId, String foodCode, String foodName,
        String foodCategoryCode, String foodCategoryName, String foodMnemonicCode,
        String isActive, String isOpen, String origin, String tagId, String areaTagName
) {
}
