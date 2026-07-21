package dev.nblucas.facialreconbackend.user;

import dev.nblucas.facialreconbackend.face.FaceEmbeddingService;
import dev.nblucas.facialreconbackend.face.FaceSimilarity;
import dev.nblucas.facialreconbackend.jooq.tables.records.TbUsersRecord;
import dev.nblucas.facialreconbackend.common.exceptions.InvalidPictureException;
import dev.nblucas.facialreconbackend.common.services.PictureStorageService;
import dev.nblucas.facialreconbackend.common.utils.EmbeddingCodec;
import dev.nblucas.facialreconbackend.common.utils.FilenameWithoutExtension;
import dev.nblucas.facialreconbackend.user.dto.CreateUserRequest;
import dev.nblucas.facialreconbackend.user.dto.CreateUsersBatchEntry;
import dev.nblucas.facialreconbackend.user.dto.CreateUsersBatchRequest;
import dev.nblucas.facialreconbackend.user.dto.CreateUsersBatchResponse;
import dev.nblucas.facialreconbackend.user.dto.IdentifyUserResponse;
import dev.nblucas.facialreconbackend.user.dto.UpdateUserRequest;
import dev.nblucas.facialreconbackend.user.dto.UserPageResponse;
import dev.nblucas.facialreconbackend.user.dto.UserPictureResponse;
import dev.nblucas.facialreconbackend.user.dto.UserResponse;
import dev.nblucas.facialreconbackend.user.dto.VerifyUserResponse;
import dev.nblucas.facialreconbackend.user.exceptions.UserNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class UserServiceImpl implements UserService {

    UserRepository userRepository;
    UserValidator userValidator;
    PictureStorageService pictureStorageService;
    FaceEmbeddingService faceEmbeddingService;
    UserIdentifier userIdentifier;
    FaceSimilarity faceSimilarity;
    UserBatchEmbeddingExtractor userBatchEmbeddingExtractor;

    @Autowired
    public UserServiceImpl(
            UserRepository userRepository,
            UserValidator userValidator,
            PictureStorageService pictureStorageService,
            FaceEmbeddingService faceEmbeddingService,
            UserIdentifier userIdentifier,
            FaceSimilarity faceSimilarity,
            UserBatchEmbeddingExtractor userBatchEmbeddingExtractor
    ) {
        this.userRepository = userRepository;
        this.userValidator = userValidator;
        this.pictureStorageService = pictureStorageService;
        this.faceEmbeddingService = faceEmbeddingService;
        this.userIdentifier = userIdentifier;
        this.faceSimilarity = faceSimilarity;
        this.userBatchEmbeddingExtractor = userBatchEmbeddingExtractor;
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

    public VerifyUserResponse verify(String cpf, MultipartFile picture) {
        this.userValidator.validateVerification(cpf, picture);

        TbUsersRecord user = userRepository.findByCpf(cpf)
                .orElseThrow(() -> new UserNotFoundException("User with given CPF not found."));

        float[] queryEmbedding = faceEmbeddingService.extractEmbedding(picture);
        float[] storedEmbedding = EmbeddingCodec.unbox(user.getEmbedding());
        float similarity = faceSimilarity.cosineSimilarity(queryEmbedding, storedEmbedding);

        return new VerifyUserResponse(faceSimilarity.isMatch(similarity));
    }

    public CreateUsersBatchResponse createBatch(CreateUsersBatchRequest request, List<MultipartFile> pictures) {
        List<CreateUsersBatchEntry> entries = request.users();
        this.userValidator.validateBatchCreation(entries, pictures);

        Map<String, MultipartFile> picturesByClientId = mapPicturesByClientId(pictures);
        Map<String, float[]> embeddingsByClientId = userBatchEmbeddingExtractor.extractAll(picturesByClientId);

        List<String> storedPicturePaths = new ArrayList<>();
        try {
            List<NewUser> newUsers = new ArrayList<>();
            for (CreateUsersBatchEntry entry : entries) {
                NewUser newUser = storeAndBuildNewUser(entry, picturesByClientId, embeddingsByClientId);
                storedPicturePaths.add(newUser.picturePath());
                newUsers.add(newUser);
            }

            List<TbUsersRecord> created = userRepository.createBatch(newUsers);
            return new CreateUsersBatchResponse(created.stream().map(this::createUserResponse).toList());
        } catch (RuntimeException createException) {
            deleteOrphanedPictures(storedPicturePaths, createException);
            throw createException;
        }
    }

    private NewUser storeAndBuildNewUser(
            CreateUsersBatchEntry entry, Map<String, MultipartFile> picturesByClientId, Map<String, float[]> embeddingsByClientId
    ) {
        String picturePath = pictureStorageService.store(picturesByClientId.get(entry.clientId()));
        Float[] embedding = EmbeddingCodec.box(embeddingsByClientId.get(entry.clientId()));
        return new NewUser(entry.name(), entry.cpf(), picturePath, embedding);
    }

    private void deleteOrphanedPictures(List<String> picturePaths, RuntimeException failure) {
        for (String picturePath : picturePaths) {
            try {
                pictureStorageService.delete(picturePath);
            } catch (RuntimeException deleteException) {
                failure.addSuppressed(deleteException);
            }
        }
    }

    private Map<String, MultipartFile> mapPicturesByClientId(List<MultipartFile> pictures) {
        Map<String, MultipartFile> picturesByClientId = new HashMap<>();
        for (MultipartFile picture : pictures) {
            String clientId = FilenameWithoutExtension.strip(picture.getOriginalFilename())
                    .orElseThrow(() -> new InvalidPictureException("Picture filename must be named after its clientId."));
            picturesByClientId.put(clientId, picture);
        }
        return picturesByClientId;
    }

    private UserResponse createUserResponse(TbUsersRecord user) {
        return new UserResponse(
                user.getCoSeqUser(), user.getName(), user.getCpf(), user.getCreatedAt(), user.getUpdatedAt());
    }
}
