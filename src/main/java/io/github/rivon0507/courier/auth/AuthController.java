package io.github.rivon0507.courier.auth;

import io.github.rivon0507.courier.auth.api.AuthenticationResponse;
import io.github.rivon0507.courier.auth.api.LoginRequest;
import io.github.rivon0507.courier.auth.api.RegisterRequest;
import io.github.rivon0507.courier.auth.service.AuthService;
import io.github.rivon0507.courier.auth.service.AuthSessionResult;
import io.github.rivon0507.courier.security.configuration.SessionProperties;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
class AuthController {

    private final AuthService authService;
    private final SessionProperties sessionProperties;

    @PostMapping("/login")
    public ResponseEntity<AuthenticationResponse> login(
            @Valid @RequestBody LoginRequest loginRequest,
            @CookieValue(name = "device_id", required = false) @Nullable String deviceId) {

        AuthSessionResult result = authService.login(loginRequest.email(), loginRequest.password(), deviceId);
        HttpHeaders cookieHeaders = buildAuthSessionHeaders(result, deviceId);
        return ResponseEntity.ok()
                .headers(cookieHeaders)
                .body(result.response());
    }

    @PostMapping("/register")
    public ResponseEntity<AuthenticationResponse> register(
            @Valid @RequestBody RegisterRequest registerRequest,
            @CookieValue(name = "device_id", required = false) @Nullable String deviceId) {

        AuthSessionResult result = authService.register(
                registerRequest.email(),
                registerRequest.password(),
                registerRequest.displayName(),
                deviceId
        );
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/users/me")
                .build()
                .toUri();
        HttpHeaders cookieHeaders = buildAuthSessionHeaders(result, deviceId);
        return ResponseEntity.created(location)
                .headers(cookieHeaders)
                .body(result.response());
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthenticationResponse> refreshSession(
            @CookieValue(name = "refresh_token", required = false) @Nullable String refreshToken,
            @CookieValue(name = "device_id", required = false) @Nullable String deviceId) {

        AuthSessionResult result = authService.refreshSession(refreshToken, deviceId);
        ResponseCookie refreshTokenCookie = buildRefreshTokenCookieFrom(result);
        return ResponseEntity.ok()
                .headers(httpHeaders -> httpHeaders.add("Set-Cookie", refreshTokenCookie.toString()))
                .body(result.response());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = "refresh_token", required = false) @Nullable String refreshToken,
            @CookieValue(name = "device_id", required = false) @Nullable String deviceId) {

        authService.logout(refreshToken, deviceId);
        return ResponseEntity.noContent()
                .headers(headers -> headers.set("Set-Cookie", clearRefreshTokenCookie().toString()))
                .build();
    }

    private @NonNull HttpHeaders buildAuthSessionHeaders(@NonNull AuthSessionResult result, String existingDeviceId) {
        HttpHeaders cookieHeaders = new HttpHeaders();
        List<String> cookieStrings = new ArrayList<>(2);
        if (!Objects.equals(existingDeviceId, result.cookies().deviceId())) {
            cookieStrings.add(buildDeviceIdCookieFrom(result).toString());
        }
        cookieStrings.add(buildRefreshTokenCookieFrom(result).toString());
        cookieHeaders.addAll("Set-Cookie", cookieStrings);
        return cookieHeaders;
    }

    private @NonNull ResponseCookie buildDeviceIdCookieFrom(AuthSessionResult result) {
        return buildSessionCookie("device_id", result.cookies().deviceId(), sessionProperties.deviceIdMaxAge());
    }

    private @NonNull ResponseCookie clearRefreshTokenCookie() {
        return buildSessionCookie("refresh_token", "", Duration.ZERO);
    }

    private @NonNull ResponseCookie buildRefreshTokenCookieFrom(AuthSessionResult result) {
        return buildSessionCookie("refresh_token", result.cookies().refreshToken(), sessionProperties.refreshTokenTtl());
    }

    private @NonNull ResponseCookie buildSessionCookie(String name, String value, Duration maxAge) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(sessionProperties.enableSecureCookies())
                .path("/")
                .maxAge(maxAge)
                .sameSite("Lax")
                .build();
    }
}
