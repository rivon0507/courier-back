package io.github.rivon0507.courier.envoi.web.error;

import io.github.rivon0507.courier.common.web.error.ApiException;
import org.springframework.http.HttpStatus;

public class EnvoiNotFoundException extends ApiException {
    public EnvoiNotFoundException() {
        super(HttpStatus.NOT_FOUND, "ENVOI_NOT_FOUND", "Envoi not found");
    }
}
