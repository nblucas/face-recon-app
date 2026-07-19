package dev.nblucas.facialreconbackend.services;

import dev.nblucas.facialreconbackend.dtos.CreateUserRequest;
import dev.nblucas.facialreconbackend.dtos.UserResponse;
import dev.nblucas.facialreconbackend.jooq.tables.records.TbUsersRecord;
import dev.nblucas.facialreconbackend.repositories.UserRepository;
import dev.nblucas.facialreconbackend.validators.UserValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class UserServiceImpl implements UserService {

    UserRepository userRepository;
    UserValidator userValidator;

    @Autowired
    public UserServiceImpl(
            UserRepository userRepository,
            UserValidator userValidator
    ) {
        this.userRepository = userRepository;
        this.userValidator = userValidator;
    }

    public UserResponse create(CreateUserRequest request, MultipartFile picture) {
        this.userValidator.validateCreation(request, picture);
        // Generate UUID for picture file name and add picture to filesystem
        // Detect face in picture (if exists, and if not, validate)
        // Extract numerical representation of picture
        TbUsersRecord user = userRepository.create(request.name(), request.cpf(), "placeholder");
        return createUserResponse(user);
    }

    private UserResponse createUserResponse(TbUsersRecord user) {
        return new UserResponse(user.getName(), user.getCpf(), user.getCreatedAt());
    }
}
