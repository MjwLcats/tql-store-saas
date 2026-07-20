package com.tql.store.integration.hrbutler;

import java.time.LocalDate;

public record HrUserRecord(
        String externalUserId,
        String externalOrgId,
        String userNumber,
        String userName,
        String userNamePinyin,
        String genderCode,
        String mobile,
        String email,
        String userType,
        String userStatus,
        String externalPositionId,
        String positionName,
        String externalPostId,
        String postName,
        String postTypeName,
        String externalRankId,
        String rankName,
        String leaderUserIds,
        LocalDate offerDate,
        LocalDate startDate,
        String rawJson
) {
}
