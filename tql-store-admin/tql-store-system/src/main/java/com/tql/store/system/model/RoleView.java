package com.tql.store.system.model;

import java.util.List;

public record RoleView(
        Long id,
        String code,
        String name,
        int status,
        String remark,
        List<Long> menuIds,
        long menuCount,
        long userCount
) {
}
