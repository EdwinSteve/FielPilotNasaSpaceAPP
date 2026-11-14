package com.nameless.nasa.domain.ports.output;

import com.nameless.nasa.domain.model.User;

import java.util.Optional;

public interface UserRepository {
    Optional<User> findByPhone(String phone);
    boolean existsByPhone(String phone);
    boolean existsByEmail(String email);
    User save(User user);
}
