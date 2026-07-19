package dev.nblucas.facialreconbackend.dtos;

import jakarta.validation.constraints.NotBlank;

public record UpdateUserRequest(
        @NotBlank String name
) {
}
