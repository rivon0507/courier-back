package io.github.rivon0507.courier.auth.api;

public record UserDto(
        String email,
        String displayName,
        String role
) {}
