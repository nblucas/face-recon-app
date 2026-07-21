package dev.nblucas.facialreconbackend.user;

import dev.nblucas.facialreconbackend.user.dto.CreateUserRequest;
import dev.nblucas.facialreconbackend.user.dto.IdentifyUserResponse;
import dev.nblucas.facialreconbackend.user.dto.UpdateUserRequest;
import dev.nblucas.facialreconbackend.user.dto.UserPageResponse;
import dev.nblucas.facialreconbackend.user.dto.UserPictureResponse;
import dev.nblucas.facialreconbackend.user.dto.UserResponse;
import dev.nblucas.facialreconbackend.user.dto.VerifyUserResponse;
import org.springframework.web.multipart.MultipartFile;

public interface UserService {
    public UserResponse create(CreateUserRequest request, MultipartFile picture);
    public UserResponse update(Long id, UpdateUserRequest request, MultipartFile picture);
    public UserPageResponse list(int offset, int limit);
    public UserPictureResponse getPicture(Long id);
    public void delete(Long id);
    public UserResponse get(Long id);
    public IdentifyUserResponse identify(MultipartFile picture);
    public VerifyUserResponse verify(String cpf, MultipartFile picture);
}
