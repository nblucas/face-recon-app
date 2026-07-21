package dev.nblucas.facialreconbackend.user.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record CreateUsersBatchRequest(
        @NotEmpty @Valid List<CreateUsersBatchEntry> users
) {
}
