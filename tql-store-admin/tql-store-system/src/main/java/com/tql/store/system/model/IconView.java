package com.tql.store.system.model;

public record IconView(Long id, String name, String code, String category, String sourceType,
                       String svgContent, int status, int order, int usageCount) {
}
