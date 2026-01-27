package io.github.rivon0507.courier.auth.api;

public record RegisterRequest(
        String email,
        String password,
        String displayName
) {
}
