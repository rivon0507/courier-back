package io.github.rivon0507.courier.auth;

import io.github.rivon0507.courier.auth.api.AuthenticationResponse;
import io.github.rivon0507.courier.auth.api.LoginRequest;
import io.github.rivon0507.courier.auth.api.RegisterRequest;
import io.github.rivon0507.courier.auth.service.AuthService;
import io.github.rivon0507.courier.auth.service.AuthSessionResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<AuthenticationResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        return ResponseEntity.ok(authService.login(loginRequest.email(), loginRequest.password()));
    }

    @PostMapping("/register")
    public ResponseEntity<AuthenticationResponse> register(@Valid @RequestBody RegisterRequest registerRequest) {
        AuthenticationResponse authenticationResponse = authService.register(
                registerRequest.email(),
                registerRequest.password(),
                registerRequest.displayName()
        );
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/users/me")
                .build()
                .toUri();
        return ResponseEntity.created(location)
                .body(authenticationResponse);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthenticationResponse> refreshSession(
            @CookieValue(name = "refresh_token", required = false) @Nullable String refreshToken,
            @CookieValue(name = "device_id", required = false) @Nullable String deviceId) {
        AuthSessionResult result = authService.refreshSession(refreshToken, deviceId);
        return ResponseEntity.ok(result.response());
    }
}
