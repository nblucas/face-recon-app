package dev.nblucas.facialreconbackend.exceptions;

public class EmptyUpdateException extends RuntimeException {
    public EmptyUpdateException(String message) {
        super(message);
    }
}
