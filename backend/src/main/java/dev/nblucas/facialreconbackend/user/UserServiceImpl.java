package dev.nblucas.facialreconbackend.user;

import dev.nblucas.facialreconbackend.face.FaceEmbeddingService;
import dev.nblucas.facialreconbackend.jooq.tables.records.TbUsersRecord;
import dev.nblucas.facialreconbackend.common.services.PictureStorageService;
import dev.nblucas.facialreconbackend.common.utils.EmbeddingCodec;
import dev.nblucas.facialreconbackend.user.dto.CreateUserRequest;
import dev.nblucas.facialreconbackend.user.dto.IdentifyUserResponse;
import dev.nblucas.facialreconbackend.user.dto.UpdateUserRequest;
import dev.nblucas.facialreconbackend.user.dto.UserPageResponse;
import dev.nblucas.facialreconbackend.user.dto.UserPictureResponse;
import dev.nblucas.facialreconbackend.user.dto.UserResponse;
import dev.nblucas.facialreconbackend.user.exceptions.UserNotFoundException;
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
    FaceEmbeddingService faceEmbeddingService;
    UserIdentifier userIdentifier;

    @Autowired
    public UserServiceImpl(
            UserRepository userRepository,
            UserValidator userValidator,
            PictureStorageService pictureStorageService,
            FaceEmbeddingService faceEmbeddingService,
            UserIdentifier userIdentifier
    ) {
        this.userRepository = userRepository;
        this.userValidator = userValidator;
        this.pictureStorageService = pictureStorageService;
        this.faceEmbeddingService = faceEmbeddingService;
        this.userIdentifier = userIdentifier;
    }

    public UserResponse create(CreateUserRequest request, MultipartFile picture) {
        this.userValidator.validateCreation(request, picture);
        Float[] embedding = EmbeddingCodec.box(faceEmbeddingService.extractEmbedding(picture));
        String picturePath = this.pictureStorageService.store(picture);

        try {
            TbUsersRecord user = userRepository.create(request.name(), request.cpf(), picturePath, embedding);
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
        Float[] embedding = resolveEmbedding(picture, existingUser);
        String picturePath = resolvePicturePath(picture, existingUser);

        try {
            TbUsersRecord user = userRepository.update(id, name, picturePath, embedding)
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

    private Float[] resolveEmbedding(MultipartFile picture, TbUsersRecord existingUser) {
        return picture != null
                ? EmbeddingCodec.box(faceEmbeddingService.extractEmbedding(picture))
                : existingUser.getEmbedding();
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

    public UserResponse get(Long id) {
        TbUsersRecord user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User with given ID not found."));
        return createUserResponse(user);
    }

    public IdentifyUserResponse identify(MultipartFile picture) {
        this.userValidator.validateIdentification(picture);
        float[] queryEmbedding = faceEmbeddingService.extractEmbedding(picture);

        return userIdentifier.findBestMatch(queryEmbedding)
                .map(user -> new IdentifyUserResponse(true, createUserResponse(user)))
                .orElseGet(() -> new IdentifyUserResponse(false, null));
    }

    private UserResponse createUserResponse(TbUsersRecord user) {
        return new UserResponse(
                user.getCoSeqUser(), user.getName(), user.getCpf(), user.getCreatedAt(), user.getUpdatedAt());
    }
}
