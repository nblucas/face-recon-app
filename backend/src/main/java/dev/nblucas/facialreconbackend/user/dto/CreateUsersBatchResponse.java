package dev.nblucas.facialreconbackend.user.dto;

import java.util.List;

public record CreateUsersBatchResponse(List<UserResponse> users) {
}
