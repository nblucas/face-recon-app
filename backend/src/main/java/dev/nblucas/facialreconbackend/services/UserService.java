package dev.nblucas.facialreconbackend.services;

import dev.nblucas.facialreconbackend.dtos.CreateUserRequest;
import dev.nblucas.facialreconbackend.dtos.UserResponse;
import org.springframework.web.multipart.MultipartFile;

public interface UserService {
    public UserResponse create(CreateUserRequest request, MultipartFile picture);
}
