package dev.nblucas.facialreconbackend.common.exceptions;

public class InvalidFaceCountException extends RuntimeException {
    public InvalidFaceCountException(String message) {
        super(message);
    }
}
