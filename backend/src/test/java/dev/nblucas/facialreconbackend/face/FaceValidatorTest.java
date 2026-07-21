package dev.nblucas.facialreconbackend.face;

import dev.nblucas.facialreconbackend.common.exceptions.InvalidFaceCountException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FaceValidatorTest {

    private final FaceValidator validator = new FaceValidator();

    @Test
    void shouldThrowInvalidFaceCountWhenNoFaceIsDetected() {
        assertThatThrownBy(() -> validator.validateFaceCount(List.of()))
                .isInstanceOf(InvalidFaceCountException.class)
                .hasMessage("No face detected in the picture given.");
    }

    @Test
    void shouldThrowInvalidFaceCountWhenMoreThanOneFaceIsDetected() {
        DetectedFace first = new DetectedFace(0, 0, 10, 10, 0.9f, new float[5][2]);
        DetectedFace second = new DetectedFace(20, 20, 30, 30, 0.8f, new float[5][2]);

        assertThatThrownBy(() -> validator.validateFaceCount(List.of(first, second)))
                .isInstanceOf(InvalidFaceCountException.class)
                .hasMessage("More than one face detected in the picture given.");
    }

    @Test
    void shouldNotThrowWhenExactlyOneFaceIsDetected() {
        DetectedFace face = new DetectedFace(0, 0, 10, 10, 0.9f, new float[5][2]);

        assertThatCode(() -> validator.validateFaceCount(List.of(face))).doesNotThrowAnyException();
    }
}
