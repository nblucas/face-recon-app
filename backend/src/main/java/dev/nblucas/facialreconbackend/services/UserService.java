package dev.nblucas.facialreconbackend.services;

import dev.nblucas.facialreconbackend.dtos.CreateUserRequest;
import dev.nblucas.facialreconbackend.dtos.UpdateUserRequest;
import dev.nblucas.facialreconbackend.dtos.UserPageResponse;
import dev.nblucas.facialreconbackend.dtos.UserPictureResponse;
import dev.nblucas.facialreconbackend.dtos.UserResponse;
import org.springframework.web.multipart.MultipartFile;

public interface UserService {
    public UserResponse create(CreateUserRequest request, MultipartFile picture);
    public UserResponse update(Long id, UpdateUserRequest request, MultipartFile picture);
    public UserPageResponse list(int offset, int limit);
    public UserPictureResponse getPicture(Long id);
}
