package dev.nblucas.facialreconbackend.dtos;

import java.time.OffsetDateTime;

public record UserResponse(
        Long id,
        String name,
        String cpf,
        OffsetDateTime createdAt
) {
}
