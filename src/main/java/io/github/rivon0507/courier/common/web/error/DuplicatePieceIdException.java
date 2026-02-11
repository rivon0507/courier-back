package io.github.rivon0507.courier.common.web.error;

import org.springframework.http.HttpStatus;

public class DuplicatePieceIdException extends ApiException {
    public DuplicatePieceIdException() {
        super(HttpStatus.BAD_REQUEST, "DUPLICATE_PIECE_ID", "Duplicate piece ID");
    }
}
