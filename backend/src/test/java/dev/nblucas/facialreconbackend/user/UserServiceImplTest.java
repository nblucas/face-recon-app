package dev.nblucas.facialreconbackend.user;

import dev.nblucas.facialreconbackend.common.exceptions.InvalidFaceCountException;
import dev.nblucas.facialreconbackend.face.FaceEmbeddingService;
import dev.nblucas.facialreconbackend.jooq.tables.records.TbUsersRecord;
import dev.nblucas.facialreconbackend.common.services.PictureStorageService;
import dev.nblucas.facialreconbackend.user.dto.CreateUserRequest;
import dev.nblucas.facialreconbackend.user.dto.UpdateUserRequest;
import dev.nblucas.facialreconbackend.user.dto.UserPageResponse;
import dev.nblucas.facialreconbackend.user.dto.UserPictureResponse;
import dev.nblucas.facialreconbackend.user.dto.UserResponse;
import dev.nblucas.facialreconbackend.user.exceptions.InvalidNameException;
import dev.nblucas.facialreconbackend.user.exceptions.InvalidPaginationException;
import dev.nblucas.facialreconbackend.user.exceptions.UserNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserValidator userValidator;

    @Mock
    private PictureStorageService pictureStorageService;

    @Mock
    private FaceEmbeddingService faceEmbeddingService;

    private UserServiceImpl userService;

    private static final float[] EXTRACTED_EMBEDDING = {0.1f, 0.2f};
    private static final Float[] BOXED_EMBEDDING = {0.1f, 0.2f};

    @BeforeEach
    void setUp() {
        userService = new UserServiceImpl(userRepository, userValidator, pictureStorageService, faceEmbeddingService);
    }

    @Test
    void shouldValidateThenCreateUserAndReturnResponse() {
        CreateUserRequest request = new CreateUserRequest("John Doe", "52998224725");
        MultipartFile picture = picture();
        OffsetDateTime createdAt = OffsetDateTime.now();
        TbUsersRecord created = new TbUsersRecord(
                1L, "John Doe", "52998224725", "generated.png", createdAt, createdAt, BOXED_EMBEDDING);

        when(faceEmbeddingService.extractEmbedding(picture)).thenReturn(EXTRACTED_EMBEDDING);
        when(pictureStorageService.store(picture)).thenReturn("generated.png");
        when(userRepository.create("John Doe", "52998224725", "generated.png", BOXED_EMBEDDING)).thenReturn(created);

        UserResponse response = userService.create(request, picture);

        InOrder inOrder = inOrder(userValidator, faceEmbeddingService, pictureStorageService, userRepository);
        inOrder.verify(userValidator).validateCreation(request, picture);
        inOrder.verify(faceEmbeddingService).extractEmbedding(picture);
        inOrder.verify(pictureStorageService).store(picture);
        inOrder.verify(userRepository).create("John Doe", "52998224725", "generated.png", BOXED_EMBEDDING);

        assertThat(response).isEqualTo(new UserResponse(1L, "John Doe", "52998224725", createdAt, createdAt));
    }

    @Test
    void shouldDeleteStoredPictureWhenRepositoryCreateFails() {
        CreateUserRequest request = new CreateUserRequest("John Doe", "52998224725");
        MultipartFile picture = picture();
        RuntimeException createFailure = new RuntimeException("duplicate key");

        when(faceEmbeddingService.extractEmbedding(picture)).thenReturn(EXTRACTED_EMBEDDING);
        when(pictureStorageService.store(picture)).thenReturn("generated.png");
        when(userRepository.create("John Doe", "52998224725", "generated.png", BOXED_EMBEDDING)).thenThrow(createFailure);

        assertThatThrownBy(() -> userService.create(request, picture)).isSameAs(createFailure);

        verify(pictureStorageService).delete("generated.png");
    }

    @Test
    void shouldSuppressCleanupFailureAndKeepOriginalExceptionWhenBothFail() {
        CreateUserRequest request = new CreateUserRequest("John Doe", "52998224725");
        MultipartFile picture = picture();
        RuntimeException createFailure = new RuntimeException("duplicate key");
        RuntimeException deleteFailure = new RuntimeException("disk error");

        when(faceEmbeddingService.extractEmbedding(picture)).thenReturn(EXTRACTED_EMBEDDING);
        when(pictureStorageService.store(picture)).thenReturn("generated.png");
        when(userRepository.create("John Doe", "52998224725", "generated.png", BOXED_EMBEDDING)).thenThrow(createFailure);
        doThrow(deleteFailure).when(pictureStorageService).delete("generated.png");

        assertThatThrownBy(() -> userService.create(request, picture)).isSameAs(createFailure);

        assertThat(createFailure.getSuppressed()).containsExactly(deleteFailure);
    }

    @Test
    void shouldNotCreateUserWhenValidationFails() {
        CreateUserRequest request = new CreateUserRequest("", "52998224725");
        MultipartFile picture = picture();

        doThrow(new InvalidNameException("Name given is invalid."))
                .when(userValidator).validateCreation(request, picture);

        assertThatThrownBy(() -> userService.create(request, picture))
                .isInstanceOf(InvalidNameException.class);

        verifyNoInteractions(faceEmbeddingService);
        verifyNoInteractions(pictureStorageService);
        verifyNoInteractions(userRepository);
    }

    @Test
    void shouldNotCreateUserWhenFaceExtractionFails() {
        CreateUserRequest request = new CreateUserRequest("John Doe", "52998224725");
        MultipartFile picture = picture();
        InvalidFaceCountException extractionFailure = new InvalidFaceCountException("No face detected in the picture given.");

        when(faceEmbeddingService.extractEmbedding(picture)).thenThrow(extractionFailure);

        assertThatThrownBy(() -> userService.create(request, picture)).isSameAs(extractionFailure);

        verifyNoInteractions(pictureStorageService);
        verifyNoInteractions(userRepository);
    }

    @Test
    void shouldValidateThenUpdateUserAndReturnResponse() {
        UpdateUserRequest request = new UpdateUserRequest("John Smith");
        MultipartFile picture = picture();
        TbUsersRecord existingUser = new TbUsersRecord(
                1L, "John Doe", "52998224725", "old.png", OffsetDateTime.now(), OffsetDateTime.now(), BOXED_EMBEDDING);
        OffsetDateTime createdAt = OffsetDateTime.now().minusDays(1);
        OffsetDateTime updatedAt = OffsetDateTime.now();
        TbUsersRecord updated = new TbUsersRecord(
                1L, "John Smith", "52998224725", "new.png", createdAt, updatedAt, BOXED_EMBEDDING);

        when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));
        when(faceEmbeddingService.extractEmbedding(picture)).thenReturn(EXTRACTED_EMBEDDING);
        when(pictureStorageService.store(picture)).thenReturn("new.png");
        when(userRepository.update(1L, "John Smith", "new.png", BOXED_EMBEDDING)).thenReturn(Optional.of(updated));

        UserResponse response = userService.update(1L, request, picture);

        InOrder inOrder = inOrder(userValidator, userRepository, faceEmbeddingService, pictureStorageService);
        inOrder.verify(userValidator).validateUpdate(1L, request, picture);
        inOrder.verify(userRepository).findById(1L);
        inOrder.verify(faceEmbeddingService).extractEmbedding(picture);
        inOrder.verify(pictureStorageService).store(picture);
        inOrder.verify(userRepository).update(1L, "John Smith", "new.png", BOXED_EMBEDDING);

        assertThat(response).isEqualTo(new UserResponse(1L, "John Smith", "52998224725", createdAt, updatedAt));
    }

    @Test
    void shouldReuseExistingPicturePathWhenNoNewPictureGiven() {
        UpdateUserRequest request = new UpdateUserRequest("John Smith");
        TbUsersRecord existingUser = new TbUsersRecord(
                1L, "John Doe", "52998224725", "existing.png", OffsetDateTime.now(), OffsetDateTime.now(), BOXED_EMBEDDING);
        TbUsersRecord updated = new TbUsersRecord(
                1L, "John Smith", "52998224725", "existing.png", OffsetDateTime.now(), OffsetDateTime.now(), BOXED_EMBEDDING);

        when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));
        when(userRepository.update(1L, "John Smith", "existing.png", BOXED_EMBEDDING)).thenReturn(Optional.of(updated));

        userService.update(1L, request, null);

        verifyNoInteractions(faceEmbeddingService);
        verifyNoInteractions(pictureStorageService);
        verify(userRepository).update(1L, "John Smith", "existing.png", BOXED_EMBEDDING);
    }

    @Test
    void shouldReuseExistingNameWhenNoNewNameGiven() {
        UpdateUserRequest request = new UpdateUserRequest(null);
        MultipartFile picture = picture();
        TbUsersRecord existingUser = new TbUsersRecord(
                1L, "John Doe", "52998224725", "old.png", OffsetDateTime.now(), OffsetDateTime.now(), BOXED_EMBEDDING);
        TbUsersRecord updated = new TbUsersRecord(
                1L, "John Doe", "52998224725", "new.png", OffsetDateTime.now(), OffsetDateTime.now(), BOXED_EMBEDDING);

        when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));
        when(faceEmbeddingService.extractEmbedding(picture)).thenReturn(EXTRACTED_EMBEDDING);
        when(pictureStorageService.store(picture)).thenReturn("new.png");
        when(userRepository.update(1L, "John Doe", "new.png", BOXED_EMBEDDING)).thenReturn(Optional.of(updated));

        userService.update(1L, request, picture);

        verify(userRepository).update(1L, "John Doe", "new.png", BOXED_EMBEDDING);
    }

    @Test
    void shouldThrowUserNotFoundWhenUserVanishesBeforeUpdateLookup() {
        UpdateUserRequest request = new UpdateUserRequest("John Smith");

        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.update(1L, request, picture()))
                .isInstanceOf(UserNotFoundException.class);

        verifyNoInteractions(pictureStorageService);
    }

    @Test
    void shouldThrowUserNotFoundWhenRecordVanishesBeforeUpdate() {
        UpdateUserRequest request = new UpdateUserRequest("John Smith");
        MultipartFile picture = picture();
        TbUsersRecord existingUser = new TbUsersRecord(
                1L, "John Doe", "52998224725", "old.png", OffsetDateTime.now(), OffsetDateTime.now(), BOXED_EMBEDDING);

        when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));
        when(faceEmbeddingService.extractEmbedding(picture)).thenReturn(EXTRACTED_EMBEDDING);
        when(pictureStorageService.store(picture)).thenReturn("new.png");
        when(userRepository.update(1L, "John Smith", "new.png", BOXED_EMBEDDING)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.update(1L, request, picture))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void shouldDeleteNewlyStoredPictureWhenUpdateFails() {
        UpdateUserRequest request = new UpdateUserRequest("John Smith");
        MultipartFile picture = picture();
        TbUsersRecord existingUser = new TbUsersRecord(
                1L, "John Doe", "52998224725", "old.png", OffsetDateTime.now(), OffsetDateTime.now(), BOXED_EMBEDDING);
        RuntimeException updateFailure = new RuntimeException("connection lost");

        when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));
        when(faceEmbeddingService.extractEmbedding(picture)).thenReturn(EXTRACTED_EMBEDDING);
        when(pictureStorageService.store(picture)).thenReturn("new.png");
        when(userRepository.update(1L, "John Smith", "new.png", BOXED_EMBEDDING)).thenThrow(updateFailure);

        assertThatThrownBy(() -> userService.update(1L, request, picture)).isSameAs(updateFailure);

        verify(pictureStorageService).delete("new.png");
    }

    @Test
    void shouldNotAttemptCleanupWhenUpdateFailsWithoutNewPicture() {
        UpdateUserRequest request = new UpdateUserRequest("John Smith");
        TbUsersRecord existingUser = new TbUsersRecord(
                1L, "John Doe", "52998224725", "existing.png", OffsetDateTime.now(), OffsetDateTime.now(), BOXED_EMBEDDING);
        RuntimeException updateFailure = new RuntimeException("connection lost");

        when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));
        when(userRepository.update(1L, "John Smith", "existing.png", BOXED_EMBEDDING)).thenThrow(updateFailure);

        assertThatThrownBy(() -> userService.update(1L, request, null)).isSameAs(updateFailure);

        verifyNoInteractions(faceEmbeddingService);
        verifyNoInteractions(pictureStorageService);
    }

    @Test
    void shouldDeleteOldPictureWhenUpdateSucceedsWithNewPicture() {
        UpdateUserRequest request = new UpdateUserRequest("John Smith");
        MultipartFile picture = picture();
        TbUsersRecord existingUser = new TbUsersRecord(
                1L, "John Doe", "52998224725", "old.png", OffsetDateTime.now(), OffsetDateTime.now(), BOXED_EMBEDDING);
        TbUsersRecord updated = new TbUsersRecord(
                1L, "John Smith", "52998224725", "new.png", OffsetDateTime.now(), OffsetDateTime.now(), BOXED_EMBEDDING);

        when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));
        when(faceEmbeddingService.extractEmbedding(picture)).thenReturn(EXTRACTED_EMBEDDING);
        when(pictureStorageService.store(picture)).thenReturn("new.png");
        when(userRepository.update(1L, "John Smith", "new.png", BOXED_EMBEDDING)).thenReturn(Optional.of(updated));

        userService.update(1L, request, picture);

        verify(pictureStorageService).delete("old.png");
    }

    @Test
    void shouldNotDeleteAnythingWhenNoNewPictureGiven() {
        UpdateUserRequest request = new UpdateUserRequest("John Smith");
        TbUsersRecord existingUser = new TbUsersRecord(
                1L, "John Doe", "52998224725", "existing.png", OffsetDateTime.now(), OffsetDateTime.now(), BOXED_EMBEDDING);
        TbUsersRecord updated = new TbUsersRecord(
                1L, "John Smith", "52998224725", "existing.png", OffsetDateTime.now(), OffsetDateTime.now(), BOXED_EMBEDDING);

        when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));
        when(userRepository.update(1L, "John Smith", "existing.png", BOXED_EMBEDDING)).thenReturn(Optional.of(updated));

        userService.update(1L, request, null);

        verifyNoInteractions(faceEmbeddingService);
        verifyNoInteractions(pictureStorageService);
    }

    @Test
    void shouldStillReturnSuccessfullyWhenDeletingOldPictureFails() {
        UpdateUserRequest request = new UpdateUserRequest("John Smith");
        MultipartFile picture = picture();
        TbUsersRecord existingUser = new TbUsersRecord(
                1L, "John Doe", "52998224725", "old.png", OffsetDateTime.now(), OffsetDateTime.now(), BOXED_EMBEDDING);
        OffsetDateTime createdAt = OffsetDateTime.now().minusDays(1);
        OffsetDateTime updatedAt = OffsetDateTime.now();
        TbUsersRecord updated = new TbUsersRecord(
                1L, "John Smith", "52998224725", "new.png", createdAt, updatedAt, BOXED_EMBEDDING);

        when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));
        when(faceEmbeddingService.extractEmbedding(picture)).thenReturn(EXTRACTED_EMBEDDING);
        when(pictureStorageService.store(picture)).thenReturn("new.png");
        when(userRepository.update(1L, "John Smith", "new.png", BOXED_EMBEDDING)).thenReturn(Optional.of(updated));
        doThrow(new RuntimeException("disk error")).when(pictureStorageService).delete("old.png");

        UserResponse response = userService.update(1L, request, picture);

        assertThat(response).isEqualTo(new UserResponse(1L, "John Smith", "52998224725", createdAt, updatedAt));
    }

    @Test
    void shouldNotUpdateUserWhenValidationFails() {
        UpdateUserRequest request = new UpdateUserRequest("");
        MultipartFile picture = picture();

        doThrow(new InvalidNameException("Name given is invalid."))
                .when(userValidator).validateUpdate(1L, request, picture);

        assertThatThrownBy(() -> userService.update(1L, request, picture))
                .isInstanceOf(InvalidNameException.class);

        verifyNoInteractions(faceEmbeddingService);
        verifyNoInteractions(userRepository);
    }

    @Test
    void shouldValidateThenListUsersAndReturnPageResponse() {
        OffsetDateTime createdAt = OffsetDateTime.now();
        TbUsersRecord first = new TbUsersRecord(
                1L, "John Doe", "52998224725", "placeholder", createdAt, createdAt, BOXED_EMBEDDING);
        TbUsersRecord second = new TbUsersRecord(
                2L, "Jane Doe", "11144477735", "placeholder", createdAt, createdAt, BOXED_EMBEDDING);

        when(userRepository.findAll(0, 20)).thenReturn(List.of(first, second));
        when(userRepository.count()).thenReturn(2L);

        UserPageResponse response = userService.list(0, 20);

        InOrder inOrder = inOrder(userValidator, userRepository);
        inOrder.verify(userValidator).validatePagination(0, 20);
        inOrder.verify(userRepository).findAll(0, 20);

        assertThat(response).isEqualTo(new UserPageResponse(
                List.of(
                        new UserResponse(1L, "John Doe", "52998224725", createdAt, createdAt),
                        new UserResponse(2L, "Jane Doe", "11144477735", createdAt, createdAt)
                ),
                2L, 0, 20
        ));
    }

    @Test
    void shouldNotListUsersWhenPaginationValidationFails() {
        doThrow(new InvalidPaginationException("Offset given can not be negative."))
                .when(userValidator).validatePagination(-1, 20);

        assertThatThrownBy(() -> userService.list(-1, 20))
                .isInstanceOf(InvalidPaginationException.class);

        verifyNoInteractions(userRepository);
    }

    @Test
    void shouldGetPngPictureAndReturnResponse() {
        TbUsersRecord user = new TbUsersRecord(
                1L, "John Doe", "52998224725", "generated.png", OffsetDateTime.now(), OffsetDateTime.now(), BOXED_EMBEDDING);
        byte[] bytes = new byte[] {1, 2, 3};

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(pictureStorageService.load("generated.png")).thenReturn(bytes);

        UserPictureResponse response = userService.getPicture(1L);

        assertThat(response).isEqualTo(new UserPictureResponse(bytes, MediaType.IMAGE_PNG));
    }

    @Test
    void shouldGetJpegPictureAndReturnResponse() {
        TbUsersRecord user = new TbUsersRecord(
                1L, "John Doe", "52998224725", "generated.jpg", OffsetDateTime.now(), OffsetDateTime.now(), BOXED_EMBEDDING);
        byte[] bytes = new byte[] {1, 2, 3};

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(pictureStorageService.load("generated.jpg")).thenReturn(bytes);

        UserPictureResponse response = userService.getPicture(1L);

        assertThat(response).isEqualTo(new UserPictureResponse(bytes, MediaType.IMAGE_JPEG));
    }

    @Test
    void shouldThrowUserNotFoundWhenGettingPictureForUnknownId() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getPicture(1L))
                .isInstanceOf(UserNotFoundException.class);

        verifyNoInteractions(pictureStorageService);
    }

    @Test
    void shouldGetUserAndReturnResponse() {
        TbUsersRecord user = new TbUsersRecord(
                1L, "John Doe", "52998224725", "generated.png", OffsetDateTime.now(), OffsetDateTime.now(), BOXED_EMBEDDING);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserResponse response = userService.get(1L);

        assertThat(response).isEqualTo(
                new UserResponse(1L, "John Doe", "52998224725", user.getCreatedAt(), user.getUpdatedAt()));
    }

    @Test
    void shouldThrowUserNotFoundWhenGettingUnknownId() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.get(1L))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void shouldDeleteUserAndItsPicture() {
        TbUsersRecord existingUser = new TbUsersRecord(
                1L, "John Doe", "52998224725", "old.png", OffsetDateTime.now(), OffsetDateTime.now(), BOXED_EMBEDDING);

        when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));

        userService.delete(1L);

        InOrder inOrder = inOrder(userRepository, pictureStorageService);
        inOrder.verify(userRepository).delete(1L);
        inOrder.verify(pictureStorageService).delete("old.png");
    }

    @Test
    void shouldThrowUserNotFoundWhenDeletingUnknownId() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.delete(1L))
                .isInstanceOf(UserNotFoundException.class);

        verifyNoInteractions(pictureStorageService);
        verify(userRepository, never()).delete(anyLong());
    }

    @Test
    void shouldNotFailWhenDeletingPictureFileFails() {
        TbUsersRecord existingUser = new TbUsersRecord(
                1L, "John Doe", "52998224725", "old.png", OffsetDateTime.now(), OffsetDateTime.now(), BOXED_EMBEDDING);

        when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));
        doThrow(new RuntimeException("disk error")).when(pictureStorageService).delete("old.png");

        assertThatCode(() -> userService.delete(1L)).doesNotThrowAnyException();
    }

    private MultipartFile picture() {
        return new MockMultipartFile("picture", "photo.png", "image/png", new byte[] {1, 2, 3});
    }
}
