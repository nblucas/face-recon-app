package dev.nblucas.facialreconbackend.services;

import dev.nblucas.facialreconbackend.dtos.CreateUserRequest;
import dev.nblucas.facialreconbackend.dtos.UpdateUserRequest;
import dev.nblucas.facialreconbackend.dtos.UserPageResponse;
import dev.nblucas.facialreconbackend.dtos.UserPictureResponse;
import dev.nblucas.facialreconbackend.dtos.UserResponse;
import dev.nblucas.facialreconbackend.exceptions.UserNotFoundException;
import dev.nblucas.facialreconbackend.jooq.tables.records.TbUsersRecord;
import dev.nblucas.facialreconbackend.repositories.UserRepository;
import dev.nblucas.facialreconbackend.validators.UserValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    UserRepository userRepository;
    UserValidator userValidator;
    PictureStorageService pictureStorageService;

    @Autowired
    public UserServiceImpl(
            UserRepository userRepository,
            UserValidator userValidator,
            PictureStorageService pictureStorageService
    ) {
        this.userRepository = userRepository;
        this.userValidator = userValidator;
        this.pictureStorageService = pictureStorageService;
    }

    public UserResponse create(CreateUserRequest request, MultipartFile picture) {
        this.userValidator.validateCreation(request, picture);
        String picturePath = this.pictureStorageService.store(picture);
        // Detect face in picture (if exists, and if not, validate)
        // Extract numerical representation of picture
        TbUsersRecord user = userRepository.create(request.name(), request.cpf(), picturePath);
        return createUserResponse(user);
    }

    public UserResponse update(Long id, UpdateUserRequest request, MultipartFile picture) {
        this.userValidator.validateUpdate(id, request, picture);
        // Generate UUID for picture file name and add picture to filesystem
        // Detect face in picture (if exists, and if not, validate)
        // Extract numerical representation of picture
        TbUsersRecord user = userRepository.update(id, request.name(), "placeholder")
                .orElseThrow(() -> new UserNotFoundException("User with given ID not found."));
        return createUserResponse(user);
    }

    public UserPageResponse list(int offset, int limit) {
        this.userValidator.validatePagination(offset, limit);

        List<UserResponse> users = userRepository.findAll(offset, limit).stream()
                .map(this::createUserResponse)
                .toList();
        long total = userRepository.count();

        return new UserPageResponse(users, total, offset, limit);
    }

    public UserPictureResponse getPicture(Long id) {
        TbUsersRecord user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User with given ID not found."));

        byte[] bytes = pictureStorageService.load(user.getPicturePath());
        MediaType contentType = user.getPicturePath().toLowerCase().endsWith(".png")
                ? MediaType.IMAGE_PNG
                : MediaType.IMAGE_JPEG;

        return new UserPictureResponse(bytes, contentType);
    }

    private UserResponse createUserResponse(TbUsersRecord user) {
        return new UserResponse(user.getCoSeqUser(), user.getName(), user.getCpf(), user.getCreatedAt());
    }
}
