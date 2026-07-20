package dev.nblucas.facialreconbackend.dtos;

import java.util.List;

public record UserPageResponse(
        List<UserResponse> users,
        long total,
        int offset,
        int limit
) {
}
