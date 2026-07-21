package dev.nblucas.facialreconbackend.common.services;

import dev.nblucas.facialreconbackend.common.exceptions.PictureStorageException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PictureStorageServiceTest {

    @Test
    void shouldCreatePicturesDirectoryOnConstruction(@TempDir Path tempDir) {
        Path picturesDir = tempDir.resolve("pictures");

        new PictureStorageService(picturesDir.toString());

        assertThat(Files.exists(picturesDir)).isTrue();
    }

    @Test
    void shouldStorePngPictureWithPngExtension(@TempDir Path tempDir) throws IOException {
        PictureStorageService service = new PictureStorageService(tempDir.toString());

        String filename = service.store(pngPicture());

        assertThat(filename).endsWith(".png");
        assertThat(Files.exists(tempDir.resolve(filename))).isTrue();
    }

    @Test
    void shouldStoreJpegPictureWithJpgExtension(@TempDir Path tempDir) throws IOException {
        PictureStorageService service = new PictureStorageService(tempDir.toString());

        String filename = service.store(jpegPicture());

        assertThat(filename).endsWith(".jpg");
    }

    @Test
    void shouldLoadPreviouslyStoredPicture(@TempDir Path tempDir) throws IOException {
        PictureStorageService service = new PictureStorageService(tempDir.toString());
        MultipartFile picture = pngPicture();
        String filename = service.store(picture);

        byte[] loaded = service.load(filename);

        assertThat(loaded).isEqualTo(picture.getBytes());
    }

    @Test
    void shouldThrowWhenLoadingMissingPicture(@TempDir Path tempDir) {
        PictureStorageService service = new PictureStorageService(tempDir.toString());

        assertThatThrownBy(() -> service.load("does-not-exist.png"))
                .isInstanceOf(PictureStorageException.class);
    }

    @Test
    void shouldDeleteStoredPicture(@TempDir Path tempDir) throws IOException {
        PictureStorageService service = new PictureStorageService(tempDir.toString());
        String filename = service.store(pngPicture());

        service.delete(filename);

        assertThat(Files.exists(tempDir.resolve(filename))).isFalse();
    }

    @Test
    void shouldNotThrowWhenDeletingMissingPicture(@TempDir Path tempDir) {
        PictureStorageService service = new PictureStorageService(tempDir.toString());

        assertThatCode(() -> service.delete("does-not-exist.png")).doesNotThrowAnyException();
    }

    @Test
    void shouldThrowWhenPictureFormatIsNotPngOrJpeg(@TempDir Path tempDir) throws IOException {
        PictureStorageService service = new PictureStorageService(tempDir.toString());
        MultipartFile picture = new MockMultipartFile(
                "picture", "photo.bmp", "image/bmp", generateImageBytes("bmp"));

        assertThatThrownBy(() -> service.store(picture))
                .isInstanceOf(PictureStorageException.class);
    }

    private MultipartFile pngPicture() throws IOException {
        return new MockMultipartFile("picture", "photo.png", "image/png", generateImageBytes("png"));
    }

    private MultipartFile jpegPicture() throws IOException {
        return new MockMultipartFile("picture", "photo.jpg", "image/jpeg", generateImageBytes("jpg"));
    }

    private byte[] generateImageBytes(String format) throws IOException {
        BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, format, out);
        return out.toByteArray();
    }
}
