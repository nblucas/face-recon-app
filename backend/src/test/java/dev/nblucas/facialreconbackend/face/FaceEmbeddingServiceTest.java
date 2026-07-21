package dev.nblucas.facialreconbackend.face;

import dev.nblucas.facialreconbackend.common.exceptions.InvalidFaceCountException;
import org.bytedeco.opencv.opencv_core.Mat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FaceEmbeddingServiceTest {

    @Mock
    private FaceDetector faceDetector;

    @Mock
    private FaceAligner faceAligner;

    @Mock
    private FaceEmbedder faceEmbedder;

    private FaceEmbeddingService service;

    @BeforeEach
    void setUp() {
        service = new FaceEmbeddingService(faceDetector, faceAligner, faceEmbedder);
    }

    @Test
    void shouldThrowInvalidFaceCountWhenNoFaceIsDetected() {
        when(faceDetector.detect(any())).thenReturn(List.of());

        assertThatThrownBy(() -> service.extractEmbedding(onePixelPngPicture()))
                .isInstanceOf(InvalidFaceCountException.class)
                .hasMessage("No face detected in the picture given.");
    }

    @Test
    void shouldThrowInvalidFaceCountWhenMoreThanOneFaceIsDetected() {
        DetectedFace first = new DetectedFace(0, 0, 10, 10, 0.9f, new float[5][2]);
        DetectedFace second = new DetectedFace(20, 20, 30, 30, 0.8f, new float[5][2]);
        when(faceDetector.detect(any())).thenReturn(List.of(first, second));

        assertThatThrownBy(() -> service.extractEmbedding(onePixelPngPicture()))
                .isInstanceOf(InvalidFaceCountException.class)
                .hasMessage("More than one face detected in the picture given.");
    }

    @Test
    void shouldAlignAndEmbedWhenExactlyOneFaceIsDetected() {
        float[][] landmarks = new float[5][2];
        DetectedFace face = new DetectedFace(0, 0, 10, 10, 0.9f, landmarks);
        Mat aligned = new Mat();
        float[] embedding = new float[512];

        when(faceDetector.detect(any())).thenReturn(List.of(face));
        when(faceAligner.align(any(), eq(landmarks))).thenReturn(aligned);
        when(faceEmbedder.embed(aligned)).thenReturn(embedding);

        float[] result = service.extractEmbedding(onePixelPngPicture());

        assertThat(result).isSameAs(embedding);
    }

    private MultipartFile onePixelPngPicture() {
        byte[] onePixelPng = {
                (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
                0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52,
                0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01,
                0x08, 0x02, 0x00, 0x00, 0x00, (byte) 0x90, 0x77, 0x53,
                (byte) 0xDE, 0x00, 0x00, 0x00, 0x0C, 0x49, 0x44, 0x41,
                0x54, 0x08, (byte) 0xD7, 0x63, (byte) 0xF8, (byte) 0xCF, (byte) 0xC0, 0x00,
                0x00, 0x03, 0x01, 0x01, 0x00, 0x18, (byte) 0xDD, (byte) 0x8D,
                (byte) 0xB0, 0x00, 0x00, 0x00, 0x00, 0x49, 0x45, 0x4E,
                0x44, (byte) 0xAE, 0x42, 0x60, (byte) 0x82
        };
        return new MockMultipartFile("picture", "photo.png", "image/png", onePixelPng);
    }
}
