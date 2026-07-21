package dev.nblucas.facialreconbackend.user;

import dev.nblucas.facialreconbackend.common.exceptions.InvalidFaceCountException;
import dev.nblucas.facialreconbackend.face.FaceEmbeddingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserBatchEmbeddingExtractorTest {

    @Mock
    private FaceEmbeddingService faceEmbeddingService;

    private UserBatchEmbeddingExtractor extractor;

    @BeforeEach
    void setUp() {
        extractor = new UserBatchEmbeddingExtractor(faceEmbeddingService);
    }

    @Test
    void shouldExtractEmbeddingForEveryPictureInTheBatch() {
        MultipartFile firstPicture = picture();
        MultipartFile secondPicture = picture();
        float[] firstEmbedding = {0.1f, 0.2f};
        float[] secondEmbedding = {0.3f, 0.4f};

        when(faceEmbeddingService.extractEmbedding(firstPicture)).thenReturn(firstEmbedding);
        when(faceEmbeddingService.extractEmbedding(secondPicture)).thenReturn(secondEmbedding);

        Map<String, float[]> result = extractor.extractAll(Map.of(
                "0", firstPicture,
                "1", secondPicture));

        assertThat(result.get("0")).isEqualTo(firstEmbedding);
        assertThat(result.get("1")).isEqualTo(secondEmbedding);
    }

    @Test
    void shouldPropagateOriginalExceptionTypeWhenExtractionFails() {
        MultipartFile picture = picture();
        InvalidFaceCountException extractionFailure =
                new InvalidFaceCountException("No face detected in the picture given.");

        when(faceEmbeddingService.extractEmbedding(picture)).thenThrow(extractionFailure);

        assertThatThrownBy(() -> extractor.extractAll(Map.of("0", picture)))
                .isSameAs(extractionFailure);
    }

    private MultipartFile picture() {
        return new MockMultipartFile("pictures", "0.png", "image/png", new byte[] {1, 2, 3});
    }
}
