package dev.nblucas.facialreconbackend.face;

import dev.nblucas.facialreconbackend.common.exceptions.InvalidPictureException;
import org.bytedeco.opencv.opencv_core.Mat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;

@Service
public class FaceEmbeddingService {

    private final FaceDetector faceDetector;
    private final FaceValidator faceValidator;
    private final FaceAligner faceAligner;
    private final FaceEmbedder faceEmbedder;

    @Autowired
    public FaceEmbeddingService(
            FaceDetector faceDetector, FaceValidator faceValidator, FaceAligner faceAligner, FaceEmbedder faceEmbedder
    ) {
        this.faceDetector = faceDetector;
        this.faceValidator = faceValidator;
        this.faceAligner = faceAligner;
        this.faceEmbedder = faceEmbedder;
    }

    public float[] extractEmbedding(MultipartFile picture) {
        BufferedImage image = readImage(picture);
        List<DetectedFace> faces = faceDetector.detect(image);
        faceValidator.validateFaceCount(faces);

        try (Mat aligned = faceAligner.align(image, faces.get(0).landmarks())) {
            return faceEmbedder.embed(aligned);
        }
    }

    private BufferedImage readImage(MultipartFile picture) {
        try {
            return ImageIO.read(picture.getInputStream());
        } catch (IOException e) {
            throw new InvalidPictureException("Could not read picture file.");
        }
    }
}
