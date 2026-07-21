package dev.nblucas.facialreconbackend.user;

import dev.nblucas.facialreconbackend.user.dto.CreateUserRequest;
import dev.nblucas.facialreconbackend.user.dto.UpdateUserRequest;
import dev.nblucas.facialreconbackend.user.exceptions.EmptyUpdateException;
import dev.nblucas.facialreconbackend.user.exceptions.InvalidCpfException;
import dev.nblucas.facialreconbackend.user.exceptions.InvalidNameException;
import dev.nblucas.facialreconbackend.user.exceptions.InvalidPaginationException;
import dev.nblucas.facialreconbackend.common.exceptions.InvalidPictureException;
import dev.nblucas.facialreconbackend.user.exceptions.UserNotFoundException;
import dev.nblucas.facialreconbackend.common.utils.ImageFormatDetector;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Component
public class UserValidator {
    private static final int MAX_PAGE_LIMIT = 20;

    UserRepository userRepository;

    @Autowired
    public UserValidator(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public void validateCreation(CreateUserRequest request, MultipartFile picture) {
        validateCpf(request.cpf());
        validateName(request.name());
        validatePicture(picture);
    }

    public void validateUpdate(Long id, UpdateUserRequest request, MultipartFile picture) {
        validateUserExists(id);

        if (request.name() == null && picture == null) {
            throw new EmptyUpdateException("At least one of name or picture must be given.");
        }

        if (request.name() != null) {
            validateName(request.name());
        }
        if (picture != null) {
            validatePicture(picture);
        }
    }

    public void validateIdentification(MultipartFile picture) {
        validatePicture(picture);
    }

    public void validatePagination(int offset, int limit) {
        if (offset < 0) {
            throw new InvalidPaginationException("Offset given can not be negative.");
        }

        if (limit < 1 || limit > MAX_PAGE_LIMIT) {
            throw new InvalidPaginationException("Limit given must be between 1 and " + MAX_PAGE_LIMIT + ".");
        }
    }

    private void validateCpf(String cpf) {
        boolean isCpfFormatInvalid = cpf == null || !cpf.matches("\\d{11}") || cpf.chars().distinct().count() == 1;

        if (isCpfFormatInvalid) {
            throw new InvalidCpfException("CPF given is invalid.");
        }

        int[] digits = cpf.chars().map(c -> c - '0').toArray();

        if (calculateCheckDigit(digits, 9) != digits[9] ||
                calculateCheckDigit(digits, 10) != digits[10]) {
            isCpfFormatInvalid = true;
        }

        if(isCpfFormatInvalid) {
            throw new InvalidCpfException("CPF given is invalid.");
        }

        if(userRepository.exists(cpf)) {
            throw new InvalidCpfException("CPF given is already registered.");
        }
    }

    private int calculateCheckDigit(int[] digits, int length) {
        int weight = length + 1;
        int sum = 0;

        for (int i = 0; i < length; i++) {
            sum += digits[i] * weight--;
        }

        int remainder = (sum * 10) % 11;
        return remainder == 10 ? 0 : remainder;
    }

    private void validateUserExists(Long id) {
        if (!userRepository.exists(id)) {
            throw new UserNotFoundException("User with given ID not found.");
        }
    }

    private void validateName(String name) {
        if(name == null || name.trim().isEmpty()) {
            throw new InvalidNameException("Name given is invalid.");
        }
    }

    private void validatePicture(MultipartFile picture) {
        if (picture.isEmpty()) {
            throw new InvalidPictureException("Picture given can not be empty.");
        }

        this.isPicturePngOrJpeg(picture);
    }

    private void isPicturePngOrJpeg(MultipartFile picture) {
        try {
            String formatName = ImageFormatDetector.detect(picture.getInputStream())
                    .orElseThrow(() -> new InvalidPictureException("File given is not a valid image."));

            if (!formatName.equals("PNG") && !formatName.equals("JPEG")) {
                throw new InvalidPictureException("File given must be PNG or JPEG.");
            }
        } catch (IOException e) {
            throw new InvalidPictureException("Could not read file given.");
        }
    }
}
