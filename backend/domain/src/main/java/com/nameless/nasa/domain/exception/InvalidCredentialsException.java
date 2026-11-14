package com.nameless.nasa.domain.exception;

public class InvalidCredentialsException extends DomainException {
    public InvalidCredentialsException() {
        super("Invalid phone or password");
    }
}
