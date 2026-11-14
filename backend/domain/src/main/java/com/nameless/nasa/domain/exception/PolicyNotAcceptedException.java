package com.nameless.nasa.domain.exception;

public class PolicyNotAcceptedException extends DomainException {
    public PolicyNotAcceptedException() {
        super("The data policy must be accepted");
    }
}
