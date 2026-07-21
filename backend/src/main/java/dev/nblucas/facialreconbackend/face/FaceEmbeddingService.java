package dev.nblucas.facialreconbackend.face;

import dev.nblucas.facialreconbackend.exceptions.InvalidFaceCountException;
import dev.nblucas.facialreconbackend.exceptions.InvalidPictureException;
import org.bytedeco.opencv.opencv_core.Mat;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;

@Service
public class FaceEmbeddingService {

    private final FaceDetector faceDetector;
    private final FaceAligner faceAligner;
    private final FaceEmbedder faceEmbedder;

    public FaceEmbeddingService(FaceDetector faceDetector, FaceAligner faceAligner, FaceEmbedder faceEmbedder) {
        this.faceDetector = faceDetector;
        this.faceAligner = faceAligner;
        this.faceEmbedder = faceEmbedder;
    }

    public float[] extractEmbedding(MultipartFile picture) {
        BufferedImage image = readImage(picture);
        List<DetectedFace> faces = faceDetector.detect(image);

        if (faces.isEmpty()) {
            throw new InvalidFaceCountException("No face detected in the picture given.");
        }
        if (faces.size() > 1) {
            throw new InvalidFaceCountException("More than one face detected in the picture given.");
        }

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
