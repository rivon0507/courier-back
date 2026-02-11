package io.github.rivon0507.courier.reception.web.error;

import io.github.rivon0507.courier.common.web.error.ApiException;
import org.springframework.http.HttpStatus;

public class ReceptionNotFoundException extends ApiException {
    public ReceptionNotFoundException() {
        super(HttpStatus.NOT_FOUND, "RECEPTION_NOT_FOUND", "Reception not found");
    }
}
