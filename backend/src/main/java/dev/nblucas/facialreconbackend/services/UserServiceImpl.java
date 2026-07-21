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

        try {
            TbUsersRecord user = userRepository.create(request.name(), request.cpf(), picturePath);
            return createUserResponse(user);
        } catch (RuntimeException createException) {
            deleteOrphanedPicture(picturePath, createException);
            throw createException;
        }
    }

    private void deleteOrphanedPicture(String picturePath, RuntimeException failure) {
        try {
            pictureStorageService.delete(picturePath);
        } catch (RuntimeException deleteException) {
            failure.addSuppressed(deleteException);
        }
    }

    public UserResponse update(Long id, UpdateUserRequest request, MultipartFile picture) {
        this.userValidator.validateUpdate(id, request, picture);

        TbUsersRecord existingUser = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User with given ID not found."));

        String name = resolveName(request, existingUser);
        String oldPicturePath = existingUser.getPicturePath();
        String picturePath = resolvePicturePath(picture, existingUser);
        // Detect face in picture (if exists, and if not, validate)
        // Extract numerical representation of picture

        try {
            TbUsersRecord user = userRepository.update(id, name, picturePath)
                    .orElseThrow(() -> new UserNotFoundException("User with given ID not found."));

            if (picture != null) {
                deletePictureBestEffort(oldPicturePath);
            }

            return createUserResponse(user);
        } catch (RuntimeException updateException) {
            if (picture != null) {
                deleteOrphanedPicture(picturePath, updateException);
            }
            throw updateException;
        }
    }

    private String resolveName(UpdateUserRequest request, TbUsersRecord existingUser) {
        return request.name() != null ? request.name() : existingUser.getName();
    }

    private String resolvePicturePath(MultipartFile picture, TbUsersRecord existingUser) {
        return picture != null
                ? this.pictureStorageService.store(picture)
                : existingUser.getPicturePath();
    }

    private void deletePictureBestEffort(String picturePath) {
        try {
            pictureStorageService.delete(picturePath);
        } catch (RuntimeException deleteException) {
            // Best-effort cleanup: the primary operation already succeeded, so a failure
            // here shouldn't turn it into an error response.
        }
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

    public void delete(Long id) {
        TbUsersRecord user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User with given ID not found."));

        userRepository.delete(id);
        deletePictureBestEffort(user.getPicturePath());
    }

    private UserResponse createUserResponse(TbUsersRecord user) {
        return new UserResponse(user.getCoSeqUser(), user.getName(), user.getCpf(), user.getCreatedAt());
    }
}
