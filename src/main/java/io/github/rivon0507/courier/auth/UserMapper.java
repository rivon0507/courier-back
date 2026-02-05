package io.github.rivon0507.courier.auth;

import io.github.rivon0507.courier.auth.api.UserDto;
import io.github.rivon0507.courier.common.domain.Role;
import io.github.rivon0507.courier.common.domain.User;
import io.github.rivon0507.courier.security.AppUserPrincipal;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "passwordHash", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "defaultWorkspace", ignore = true)
    User from(String email, String displayName, Role role);

    AppUserPrincipal toUserPrincipal(User user);

    UserDto toUserDto(User user);

    UserDto principalToUserDto(AppUserPrincipal principal);
}
