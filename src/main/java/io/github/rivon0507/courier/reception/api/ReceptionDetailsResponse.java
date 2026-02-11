package io.github.rivon0507.courier.reception.api;

import io.github.rivon0507.courier.common.api.PieceResponse;

import java.util.List;

public record ReceptionDetailsResponse(
        ReceptionResponse reception,
        List<PieceResponse> pieces
) {
}
