package dev.nblucas.facialreconbackend.user.exceptions;

public class InvalidPaginationException extends RuntimeException {
    public InvalidPaginationException(String message) {
        super(message);
    }
}
