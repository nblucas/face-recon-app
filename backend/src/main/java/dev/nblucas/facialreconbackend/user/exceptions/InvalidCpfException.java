package dev.nblucas.facialreconbackend.user.exceptions;

public class InvalidCpfException extends RuntimeException {
    public InvalidCpfException(String message) {
        super(message);
    }
}
