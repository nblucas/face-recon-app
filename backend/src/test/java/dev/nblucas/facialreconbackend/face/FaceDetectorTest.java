package dev.nblucas.facialreconbackend.face;

import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.NDManager;
import ai.djl.ndarray.types.Shape;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FaceDetectorTest {

    private final FaceDetector detector = new FaceDetector();

    @Test
    void shouldFilterOutDetectionsBelowScoreThreshold() {
        try (NDManager manager = NDManager.newBaseManager()) {
            NDList output = stride8Output(manager, new float[] {0.3f, 0.1f});

            List<DetectedFace> result = detector.decode(output, 1.0f);

            assertThat(result).isEmpty();
        }
    }

    @Test
    void shouldKeepDetectionAboveScoreThreshold() {
        try (NDManager manager = NDManager.newBaseManager()) {
            NDList output = stride8Output(manager, new float[] {0.9f, 0.1f});

            List<DetectedFace> result = detector.decode(output, 1.0f);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).score()).isEqualTo(0.9f);
        }
    }

    @Test
    void shouldSuppressOverlappingDetectionsKeepingHighestScore() {
        try (NDManager manager = NDManager.newBaseManager()) {
            NDList output = stride8Output(manager, new float[] {0.95f, 0.8f});

            List<DetectedFace> result = detector.decode(output, 1.0f);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).score()).isEqualTo(0.95f);
        }
    }

    private NDList stride8Output(NDManager manager, float[] stride8Scores) {
        int n = stride8Scores.length;
        NDArray scores8 = manager.create(stride8Scores, new Shape(n, 1));
        NDArray bbox8 = manager.ones(new Shape(n, 4));
        NDArray kps8 = manager.zeros(new Shape(n, 10));

        NDArray emptyScore = manager.create(new float[] {0f, 0f}, new Shape(2, 1));
        NDArray emptyBbox = manager.zeros(new Shape(2, 4));
        NDArray emptyKps = manager.zeros(new Shape(2, 10));

        return new NDList(
                scores8, emptyScore, emptyScore,
                bbox8, emptyBbox, emptyBbox,
                kps8, emptyKps, emptyKps
        );
    }
}
