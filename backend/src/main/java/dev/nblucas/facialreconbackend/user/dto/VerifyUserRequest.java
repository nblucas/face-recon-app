package dev.nblucas.facialreconbackend.user.dto;

import jakarta.validation.constraints.NotBlank;

public record VerifyUserRequest(
        @NotBlank String cpf
) {
}
