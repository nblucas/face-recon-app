package dev.nblucas.facialreconbackend.common.exceptions;

public class InvalidPictureException extends RuntimeException {
    public InvalidPictureException(String message) {
        super(message);
    }
}
