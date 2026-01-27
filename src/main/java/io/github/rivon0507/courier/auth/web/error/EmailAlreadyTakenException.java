package io.github.rivon0507.courier.auth.web.error;

import io.github.rivon0507.courier.common.web.error.ApiException;
import org.springframework.http.HttpStatus;

public class EmailAlreadyTakenException extends ApiException {
    public EmailAlreadyTakenException() {
        super(HttpStatus.CONFLICT, "EMAIL_ALREADY_TAKEN", "The email is already taken");
    }
}
