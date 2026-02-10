package io.github.rivon0507.courier.envoi;

import io.github.rivon0507.courier.common.web.error.ApiException;
import org.springframework.http.HttpStatus;

public class EnvoiPieceNotFoundException extends ApiException {
    public EnvoiPieceNotFoundException() {
        super(HttpStatus.NOT_FOUND, "ENVOI_PIECE_NOT_FOUND", "Envoi's piece not found");
    }
}
