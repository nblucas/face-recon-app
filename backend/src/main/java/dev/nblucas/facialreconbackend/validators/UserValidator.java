package dev.nblucas.facialreconbackend.validators;

import dev.nblucas.facialreconbackend.dtos.CreateUserRequest;
import dev.nblucas.facialreconbackend.dtos.UpdateUserRequest;
import dev.nblucas.facialreconbackend.exceptions.InvalidCpfException;
import dev.nblucas.facialreconbackend.exceptions.InvalidNameException;
import dev.nblucas.facialreconbackend.exceptions.InvalidPictureException;
import dev.nblucas.facialreconbackend.exceptions.UserNotFoundException;
import dev.nblucas.facialreconbackend.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.util.Iterator;

@Component
public class UserValidator {
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
        validateName(request.name());
        validatePicture(picture);
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
        try (ImageInputStream imageInputStream = ImageIO.createImageInputStream(picture.getInputStream())) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(imageInputStream);

            if (!readers.hasNext()) {
                throw new InvalidPictureException("File given is not a valid image.");
            }

            String formatName = readers.next().getFormatName().toUpperCase();

            if (!formatName.equals("PNG") && !formatName.equals("JPEG")) {
                throw new InvalidPictureException("File given must be PNG or JPEG.");
            }
        } catch (IOException e) {
            throw new InvalidPictureException("Could not read file given.");
        }
    }
}
