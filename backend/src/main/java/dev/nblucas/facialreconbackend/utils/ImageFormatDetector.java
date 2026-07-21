package dev.nblucas.facialreconbackend.utils;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Optional;

public final class ImageFormatDetector {

    private ImageFormatDetector() {
    }

    public static Optional<String> detect(InputStream inputStream) throws IOException {
        try (ImageInputStream imageInputStream = ImageIO.createImageInputStream(inputStream)) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(imageInputStream);

            if (!readers.hasNext()) {
                return Optional.empty();
            }

            return Optional.of(readers.next().getFormatName().toUpperCase());
        }
    }
}
