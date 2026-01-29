package io.github.rivon0507.courier.auth.web.error;

import io.github.rivon0507.courier.common.web.error.ApiException;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class InvalidSessionException extends ApiException {
    private final String reason;

    public InvalidSessionException(String reason, String code) {
        super(HttpStatus.UNAUTHORIZED, code, "Invalid session");
        this.reason = reason;
    }

    public InvalidSessionException(String reason) {
        this(reason, "INVALID_SESSION");
    }
}
