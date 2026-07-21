package dev.nblucas.facialreconbackend.face;

import dev.nblucas.facialreconbackend.common.exceptions.InvalidFaceCountException;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
class FaceValidator {

    void validateFaceCount(List<DetectedFace> faces) {
        if (faces.isEmpty()) {
            throw new InvalidFaceCountException("No face detected in the picture given.");
        }
        if (faces.size() > 1) {
            throw new InvalidFaceCountException("More than one face detected in the picture given.");
        }
    }
}
