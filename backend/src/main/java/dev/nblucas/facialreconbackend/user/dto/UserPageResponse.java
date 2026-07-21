package dev.nblucas.facialreconbackend.user.dto;

import java.util.List;

public record UserPageResponse(
        List<UserResponse> users,
        long total,
        int offset,
        int limit
) {
}
