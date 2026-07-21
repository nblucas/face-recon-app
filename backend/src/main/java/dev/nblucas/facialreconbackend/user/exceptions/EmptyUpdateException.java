package dev.nblucas.facialreconbackend.user.exceptions;

public class EmptyUpdateException extends RuntimeException {
    public EmptyUpdateException(String message) {
        super(message);
    }
}
