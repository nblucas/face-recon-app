package dev.nblucas.facialreconbackend.user.exceptions;

public class InvalidBatchSizeException extends RuntimeException {
    public InvalidBatchSizeException(String message) {
        super(message);
    }
}
