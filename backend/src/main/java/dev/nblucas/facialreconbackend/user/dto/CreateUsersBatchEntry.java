package dev.nblucas.facialreconbackend.user.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateUsersBatchEntry(
        @NotBlank String clientId,
        @NotBlank String name,
        @NotBlank String cpf
) {
}
