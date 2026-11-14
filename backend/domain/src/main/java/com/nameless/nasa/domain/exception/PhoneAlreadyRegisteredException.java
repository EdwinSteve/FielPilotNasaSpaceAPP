package com.nameless.nasa.domain.exception;

public class PhoneAlreadyRegisteredException extends DomainException {
    public PhoneAlreadyRegisteredException() {
        super("Phone number is already registered");
    }
}
