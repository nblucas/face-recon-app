package dev.nblucas.facialreconbackend.dtos;

import java.time.OffsetDateTime;

public record UserResponse(
        String name,
        String cpf,
        OffsetDateTime createdAt
) {
}
