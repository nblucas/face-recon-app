package dev.nblucas.facialreconbackend.dtos;

import jakarta.validation.constraints.NotBlank;

public record CreateUserRequest(
        @NotBlank String name,
        @NotBlank String cpf
) {
}
