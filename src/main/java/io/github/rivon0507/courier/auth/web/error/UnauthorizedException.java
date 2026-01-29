package io.github.rivon0507.courier.auth.web.error;

import io.github.rivon0507.courier.common.web.error.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;

public class UnauthorizedException extends ApiException {
    public UnauthorizedException(AuthenticationException e) {
        super(HttpStatus.UNAUTHORIZED, "AUTH_UNAUTHORIZED", e.getMessage());
    }

    public UnauthorizedException(String code) {
        super(HttpStatus.UNAUTHORIZED, code, code);
    }
}
