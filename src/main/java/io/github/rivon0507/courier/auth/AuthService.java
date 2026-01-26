package io.github.rivon0507.courier.auth;

import io.github.rivon0507.courier.auth.api.AuthenticationResponse;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
class AuthService {

    public AuthenticationResponse login(@NonNull String username, @NonNull String password) {
        throw new UnsupportedOperationException("Not implemented");
    }
}
