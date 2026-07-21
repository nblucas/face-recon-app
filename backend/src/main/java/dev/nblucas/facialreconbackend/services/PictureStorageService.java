package dev.nblucas.facialreconbackend.services;

import dev.nblucas.facialreconbackend.exceptions.PictureStorageException;
import dev.nblucas.facialreconbackend.utils.ImageFormatDetector;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Service
public class PictureStorageService {

    private final Path picturesDir;

    @Autowired
    public PictureStorageService(@Value("${app.storage.pictures-dir}") String picturesDir) {
        this.picturesDir = Path.of(picturesDir);

        try {
            Files.createDirectories(this.picturesDir);
        } catch (IOException e) {
            throw new PictureStorageException("Could not create picture storage directory.");
        }
    }

    public String store(MultipartFile picture) {
        String filename = UUID.randomUUID() + "." + detectExtension(picture);

        try {
            Files.copy(picture.getInputStream(), picturesDir.resolve(filename));
        } catch (IOException e) {
            throw new PictureStorageException("Could not store picture file.");
        }

        return filename;
    }

    public byte[] load(String picturePath) {
        try {
            return Files.readAllBytes(picturesDir.resolve(picturePath));
        } catch (IOException e) {
            throw new PictureStorageException("Could not read picture file.");
        }
    }

    private String detectExtension(MultipartFile picture) {
        try {
            String formatName = ImageFormatDetector.detect(picture.getInputStream())
                    .orElseThrow(() -> new PictureStorageException("Could not detect picture format."));

            return switch (formatName) {
                case "PNG" -> "png";
                case "JPEG" -> "jpg";
                default -> throw new PictureStorageException("Unsupported picture format: " + formatName);
            };
        } catch (IOException e) {
            throw new PictureStorageException("Could not read picture file.");
        }
    }
}
