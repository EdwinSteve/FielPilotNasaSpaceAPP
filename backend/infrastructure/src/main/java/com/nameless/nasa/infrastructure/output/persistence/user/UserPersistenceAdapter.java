package com.nameless.nasa.infrastructure.output.persistence.user;

import com.nameless.nasa.domain.model.User;
import com.nameless.nasa.domain.ports.output.UserRepository;
import com.nameless.nasa.infrastructure.output.persistence.user.mapper.UserPersistenceMapper;
import com.nameless.nasa.infrastructure.output.persistence.user.repository.UserJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class UserPersistenceAdapter implements UserRepository {
    private final UserJpaRepository jpaRepository;
    private final UserPersistenceMapper mapper;

    @Override
    public Optional<User> findByPhone(String phone) {
        return jpaRepository.findByPhone(phone).map(mapper::toDomain);
    }

    @Override
    public boolean existsByPhone(String phone) {
        return jpaRepository.existsByPhone(phone);
    }

    @Override
    public boolean existsByEmail(String email) {
        return jpaRepository.existsByEmail(email);
    }

    @Override
    public User save(User user) {
        return mapper.toDomain(jpaRepository.save(mapper.toJpaEntity(user)));
    }
}
