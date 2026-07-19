package dev.nblucas.facialreconbackend.validators;

import dev.nblucas.facialreconbackend.dtos.CreateUserRequest;
import dev.nblucas.facialreconbackend.dtos.UpdateUserRequest;
import dev.nblucas.facialreconbackend.exceptions.InvalidCpfException;
import dev.nblucas.facialreconbackend.exceptions.InvalidNameException;
import dev.nblucas.facialreconbackend.exceptions.InvalidPictureException;
import dev.nblucas.facialreconbackend.exceptions.UserNotFoundException;
import dev.nblucas.facialreconbackend.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserValidatorTest {

    @Mock
    private UserRepository userRepository;

    private UserValidator userValidator;

    @BeforeEach
    void setUp() {
        userValidator = new UserValidator(userRepository);
    }

    @ParameterizedTest(name = "[{index}] cpf={0}")
    @ValueSource(strings = {
            "52998224725",
            "11144477735",
            "12345678909",
    })
    void shouldNotThrowWhenCpfIsValid(String cpf) {
        CreateUserRequest request = new CreateUserRequest("John Doe", cpf);
        MultipartFile picture = validPicture();

        when(userRepository.exists(anyString())).thenReturn(false);

        assertThatCode(() -> userValidator.validateCreation(request, picture))
                .doesNotThrowAnyException();
    }

    @ParameterizedTest(name = "[{index}] cpf={0}")
    @NullSource
    @ValueSource(strings = {"", "   "})
    void shouldThrowInvalidCpfExceptionWhenCpfIsNullOrBlank(String cpf) {
        CreateUserRequest request = new CreateUserRequest("John Doe", cpf);
        MultipartFile picture = validPicture();

        assertThatThrownBy(() -> userValidator.validateCreation(request, picture))
                .isInstanceOf(InvalidCpfException.class)
                .hasMessage("CPF given is invalid.");
    }

    @ParameterizedTest(name = "[{index}] cpf={0}")
    @ValueSource(strings = {
            "123456789123",
            "1234567891",
            "123",
    })
    void shouldThrowInvalidCpfExceptionWhenCpfHasAnyLengthOtherThan11(String cpf) {
        CreateUserRequest request = new CreateUserRequest("John Doe", cpf);
        MultipartFile picture = validPicture();

        assertThatThrownBy(() -> userValidator.validateCreation(request, picture))
                .isInstanceOf(InvalidCpfException.class)
                .hasMessage("CPF given is invalid.");
    }

    @ParameterizedTest(name = "[{index}] cpf={0}")
    @ValueSource(strings = {
            "00000000000",
            "11111111111",
            "22222222222",
            "99999999999",
    })
    void shouldThrowInvalidCpfExceptionWhenCpfDigitsAreAllEqual(String cpf) {
        CreateUserRequest request = new CreateUserRequest("John Doe", cpf);
        MultipartFile picture = validPicture();

        assertThatThrownBy(() -> userValidator.validateCreation(request, picture))
                .isInstanceOf(InvalidCpfException.class)
                .hasMessage("CPF given is invalid.");
    }

    @ParameterizedTest(name = "[{index}] cpf={0}")
    @ValueSource(strings = {
            "52998224721",
            "11144477730",
            "12345678900",
    })
    void shouldThrowInvalidCpfExceptionWhenCpfHasInvalidCheckDigit(String cpf) {
        CreateUserRequest request = new CreateUserRequest("John Doe", cpf);
        MultipartFile picture = validPicture();

        assertThatThrownBy(() -> userValidator.validateCreation(request, picture))
                .isInstanceOf(InvalidCpfException.class)
                .hasMessage("CPF given is invalid.");
    }

    @Test
    void shouldThrowInvalidCpfExceptionWhenCpfAlreadyExistsInDatabase() {
        String validCpfButRepeated = "11144477735";
        CreateUserRequest request = new CreateUserRequest("John Doe", validCpfButRepeated);
        MultipartFile picture = validPicture();

        when(userRepository.exists(validCpfButRepeated)).thenReturn(true);

        assertThatThrownBy(() -> userValidator.validateCreation(request, picture))
                .isInstanceOf(InvalidCpfException.class)
                .hasMessage("CPF given is already registered.");
    }

    @ParameterizedTest(name = "[{index}] name={0}")
    @ValueSource(strings = {
            "John",
            "Maria",
    })
    void shouldNotThrowWhenNameIsValid(String name) {
        CreateUserRequest request = new CreateUserRequest(name, "11144477735");
        MultipartFile picture = validPicture();

        assertThatCode(() -> userValidator.validateCreation(request, picture))
                .doesNotThrowAnyException();
    }

    @ParameterizedTest(name = "[{index}] name={0}")
    @NullSource
    @ValueSource(strings = {
            "",
            "  ",
            "         "
    })
    void shouldThrowInvalidNameExceptionWhenNameIsEmptyOrNull(String name) {
        CreateUserRequest request = new CreateUserRequest(name, "11144477735");
        MultipartFile picture = validPicture();

        when(userRepository.exists(anyString())).thenReturn(false);

        assertThatThrownBy(() -> userValidator.validateCreation(request, picture))
                .isInstanceOf(InvalidNameException.class)
                .hasMessage("Name given is invalid.");
    }

    @Test
    void shouldNotThrowWhenPictureIsValidPng() {
        CreateUserRequest request = new CreateUserRequest("John Doe", "11144477735");
        MultipartFile picture = new MockMultipartFile("picture", "photo.png", "image/png", imageBytes("png"));

        when(userRepository.exists(anyString())).thenReturn(false);

        assertThatCode(() -> userValidator.validateCreation(request, picture))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldNotThrowWhenPictureIsValidJpeg() {
        CreateUserRequest request = new CreateUserRequest("John Doe", "11144477735");
        MultipartFile picture = new MockMultipartFile("picture", "photo.jpg", "image/jpeg", imageBytes("jpg"));

        when(userRepository.exists(anyString())).thenReturn(false);

        assertThatCode(() -> userValidator.validateCreation(request, picture))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldThrowInvalidPictureExceptionWhenPictureIsAnotherFormat() {
        CreateUserRequest request = new CreateUserRequest("John Doe", "11144477735");
        MultipartFile picture = new MockMultipartFile("picture", "photo.bmp", "image/bmp", imageBytes("bmp"));

        when(userRepository.exists(anyString())).thenReturn(false);

        assertThatThrownBy(() -> userValidator.validateCreation(request, picture))
                .isInstanceOf(InvalidPictureException.class)
                .hasMessage("File given must be PNG or JPEG.");
    }

    @Test
    void shouldNotThrowWhenUpdateIsValid() {
        UpdateUserRequest request = new UpdateUserRequest("John Doe");
        MultipartFile picture = validPicture();

        when(userRepository.exists(1L)).thenReturn(true);

        assertThatCode(() -> userValidator.validateUpdate(1L, request, picture))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldThrowUserNotFoundExceptionWhenIdDoesNotExistOnUpdate() {
        UpdateUserRequest request = new UpdateUserRequest("John Doe");
        MultipartFile picture = validPicture();

        when(userRepository.exists(1L)).thenReturn(false);

        assertThatThrownBy(() -> userValidator.validateUpdate(1L, request, picture))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("User with given ID not found.");
    }

    @ParameterizedTest(name = "[{index}] name={0}")
    @NullSource
    @ValueSource(strings = {
            "",
            "  ",
            "         "
    })
    void shouldThrowInvalidNameExceptionWhenNameIsEmptyOrNullOnUpdate(String name) {
        UpdateUserRequest request = new UpdateUserRequest(name);
        MultipartFile picture = validPicture();

        when(userRepository.exists(1L)).thenReturn(true);

        assertThatThrownBy(() -> userValidator.validateUpdate(1L, request, picture))
                .isInstanceOf(InvalidNameException.class)
                .hasMessage("Name given is invalid.");
    }

    @Test
    void shouldThrowInvalidPictureExceptionWhenPictureIsAnotherFormatOnUpdate() {
        UpdateUserRequest request = new UpdateUserRequest("John Doe");
        MultipartFile picture = new MockMultipartFile("picture", "photo.bmp", "image/bmp", imageBytes("bmp"));

        when(userRepository.exists(1L)).thenReturn(true);

        assertThatThrownBy(() -> userValidator.validateUpdate(1L, request, picture))
                .isInstanceOf(InvalidPictureException.class)
                .hasMessage("File given must be PNG or JPEG.");
    }

    private MultipartFile validPicture() {
        return new MockMultipartFile("picture", "photo.png", "image/png", imageBytes("png"));
    }

    private byte[] imageBytes(String formatName) {
        try {
            BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageIO.write(image, formatName, outputStream);
            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
