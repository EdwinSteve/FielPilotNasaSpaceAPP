package com.nameless.nasa.infrastructure.output.persistence.user.mapper;

import com.nameless.nasa.domain.model.User;
import com.nameless.nasa.infrastructure.output.persistence.user.model.UserEntity;
import org.mapstruct.Mapper;

@Mapper
public interface UserPersistenceMapper {
    User toDomain(UserEntity entity);
    UserEntity toJpaEntity(User domain);
}
