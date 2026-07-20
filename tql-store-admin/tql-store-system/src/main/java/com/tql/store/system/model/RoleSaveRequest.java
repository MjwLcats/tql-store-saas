package com.tql.store.system.model;

import java.util.List;

public record RoleSaveRequest(
        String roleCode,
        String roleName,
        Integer status,
        String remark,
        List<Long> menuIds
) {
}
