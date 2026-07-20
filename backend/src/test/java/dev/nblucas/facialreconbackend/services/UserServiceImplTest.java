package dev.nblucas.facialreconbackend.services;

import dev.nblucas.facialreconbackend.dtos.CreateUserRequest;
import dev.nblucas.facialreconbackend.dtos.UpdateUserRequest;
import dev.nblucas.facialreconbackend.dtos.UserPageResponse;
import dev.nblucas.facialreconbackend.dtos.UserResponse;
import dev.nblucas.facialreconbackend.exceptions.InvalidNameException;
import dev.nblucas.facialreconbackend.exceptions.InvalidPaginationException;
import dev.nblucas.facialreconbackend.jooq.tables.records.TbUsersRecord;
import dev.nblucas.facialreconbackend.repositories.UserRepository;
import dev.nblucas.facialreconbackend.validators.UserValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserValidator userValidator;

    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        userService = new UserServiceImpl(userRepository, userValidator);
    }

    @Test
    void shouldValidateThenCreateUserAndReturnResponse() {
        CreateUserRequest request = new CreateUserRequest("John Doe", "52998224725");
        MultipartFile picture = picture();
        OffsetDateTime createdAt = OffsetDateTime.now();
        TbUsersRecord created = new TbUsersRecord(
                1L, "John Doe", "52998224725", "placeholder", createdAt, createdAt);

        when(userRepository.create("John Doe", "52998224725", "placeholder")).thenReturn(created);

        UserResponse response = userService.create(request, picture);

        InOrder inOrder = inOrder(userValidator, userRepository);
        inOrder.verify(userValidator).validateCreation(request, picture);
        inOrder.verify(userRepository).create("John Doe", "52998224725", "placeholder");

        assertThat(response).isEqualTo(new UserResponse(1L, "John Doe", "52998224725", createdAt));
    }

    @Test
    void shouldNotCreateUserWhenValidationFails() {
        CreateUserRequest request = new CreateUserRequest("", "52998224725");
        MultipartFile picture = picture();

        doThrow(new InvalidNameException("Name given is invalid."))
                .when(userValidator).validateCreation(request, picture);

        assertThatThrownBy(() -> userService.create(request, picture))
                .isInstanceOf(InvalidNameException.class);

        verifyNoInteractions(userRepository);
    }

    @Test
    void shouldValidateThenUpdateUserAndReturnResponse() {
        UpdateUserRequest request = new UpdateUserRequest("John Smith");
        MultipartFile picture = picture();
        OffsetDateTime createdAt = OffsetDateTime.now().minusDays(1);
        OffsetDateTime updatedAt = OffsetDateTime.now();
        TbUsersRecord updated = new TbUsersRecord(
                1L, "John Smith", "52998224725", "placeholder", createdAt, updatedAt);

        when(userRepository.update(1L, "John Smith", "placeholder")).thenReturn(updated);

        UserResponse response = userService.update(1L, request, picture);

        InOrder inOrder = inOrder(userValidator, userRepository);
        inOrder.verify(userValidator).validateUpdate(1L, request, picture);
        inOrder.verify(userRepository).update(1L, "John Smith", "placeholder");

        assertThat(response).isEqualTo(new UserResponse(1L, "John Smith", "52998224725", createdAt));
    }

    @Test
    void shouldNotUpdateUserWhenValidationFails() {
        UpdateUserRequest request = new UpdateUserRequest("");
        MultipartFile picture = picture();

        doThrow(new InvalidNameException("Name given is invalid."))
                .when(userValidator).validateUpdate(1L, request, picture);

        assertThatThrownBy(() -> userService.update(1L, request, picture))
                .isInstanceOf(InvalidNameException.class);

        verifyNoInteractions(userRepository);
    }

    @Test
    void shouldValidateThenListUsersAndReturnPageResponse() {
        OffsetDateTime createdAt = OffsetDateTime.now();
        TbUsersRecord first = new TbUsersRecord(1L, "John Doe", "52998224725", "placeholder", createdAt, createdAt);
        TbUsersRecord second = new TbUsersRecord(2L, "Jane Doe", "11144477735", "placeholder", createdAt, createdAt);

        when(userRepository.findAll(0, 20)).thenReturn(List.of(first, second));
        when(userRepository.count()).thenReturn(2L);

        UserPageResponse response = userService.list(0, 20);

        InOrder inOrder = inOrder(userValidator, userRepository);
        inOrder.verify(userValidator).validatePagination(0, 20);
        inOrder.verify(userRepository).findAll(0, 20);

        assertThat(response).isEqualTo(new UserPageResponse(
                List.of(
                        new UserResponse(1L, "John Doe", "52998224725", createdAt),
                        new UserResponse(2L, "Jane Doe", "11144477735", createdAt)
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

    private MultipartFile picture() {
        return new MockMultipartFile("picture", "photo.png", "image/png", new byte[] {1, 2, 3});
    }
}
