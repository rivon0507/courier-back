package io.github.rivon0507.courier.auth.service;

import io.github.rivon0507.courier.auth.api.AuthenticationResponse;

public record AuthSessionResult(
        AuthenticationResponse response,
        RefreshCookies cookies
) {
    public record RefreshCookies(
            String refreshToken, String deviceId
    ) {
    }
}
