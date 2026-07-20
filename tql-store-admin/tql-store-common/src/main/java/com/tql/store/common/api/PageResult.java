package com.tql.store.common.api;

import java.util.List;

public record PageResult<T>(List<T> records, long total, int page, int pageSize) {
}
