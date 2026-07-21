package dev.nblucas.facialreconbackend.user.dto;

import java.time.OffsetDateTime;

public record UserResponse(
        Long id,
        String name,
        String cpf,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
