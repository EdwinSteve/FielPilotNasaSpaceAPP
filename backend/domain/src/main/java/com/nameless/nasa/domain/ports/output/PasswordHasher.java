package com.nameless.nasa.domain.ports.output;

public interface PasswordHasher {
    String hash(String rawPassword);
    boolean matches(String rawPassword, String hash);
}
