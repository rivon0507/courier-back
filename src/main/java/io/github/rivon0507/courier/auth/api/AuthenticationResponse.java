package io.github.rivon0507.courier.auth.api;

public record AuthenticationResponse(
        String accessToken,
        String tokenType,
        Long expiresIn,
        UserDto user
) {
}
