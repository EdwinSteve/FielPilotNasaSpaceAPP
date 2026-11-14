package com.nameless.nasa.infrastructure.output.persistence.user.repository;

import com.nameless.nasa.infrastructure.output.persistence.user.model.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserJpaRepository extends JpaRepository<UserEntity, UUID> {
    Optional<UserEntity> findByPhone(String phone);
    boolean existsByPhone(String phone);
    boolean existsByEmail(String email);
}
