package com.nameless.nasa.domain.exception;

public class EmailAlreadyRegisteredException extends DomainException {
    public EmailAlreadyRegisteredException() {
        super("Email is already registered");
    }
}
