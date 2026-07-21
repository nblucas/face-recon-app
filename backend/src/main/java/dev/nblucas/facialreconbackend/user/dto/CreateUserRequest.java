package dev.nblucas.facialreconbackend.user.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateUserRequest(
        @NotBlank String name,
        @NotBlank String cpf
) {
}
