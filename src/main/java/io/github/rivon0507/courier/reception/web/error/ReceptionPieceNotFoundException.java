package io.github.rivon0507.courier.reception.web.error;

import io.github.rivon0507.courier.common.web.error.ApiException;
import org.springframework.http.HttpStatus;

public class ReceptionPieceNotFoundException extends ApiException {
    public ReceptionPieceNotFoundException() {
        super(HttpStatus.NOT_FOUND, "RECEPTION_PIECE_NOT_FOUND", "Reception's piece not found");
    }
}
