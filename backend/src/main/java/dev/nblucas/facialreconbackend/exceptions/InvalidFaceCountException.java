package dev.nblucas.facialreconbackend.exceptions;

public class InvalidFaceCountException extends RuntimeException {
    public InvalidFaceCountException(String message) {
        super(message);
    }
}
