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
        return ResponseCookie.from("device_id", result.cookies().deviceId())
                .httpOnly(true)
                .secure(sessionProperties.enableSecureCookies())
                .path("/")
                .maxAge(sessionProperties.deviceIdMaxAge())
                .sameSite("Lax")
                .build();
    }

    private @NonNull ResponseCookie buildRefreshTokenCookieFrom(AuthSessionResult result) {
        return ResponseCookie.from("refresh_token", result.cookies().refreshToken())
                .httpOnly(true)
                .secure(sessionProperties.enableSecureCookies())
                .path("/")
                .maxAge(sessionProperties.refreshTokenTtl())
                .sameSite("Lax")
                .build();
    }
}
