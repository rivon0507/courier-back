package io.github.rivon0507.courier.envoi.api;

import java.util.List;

public record EnvoiDetailsResponse(
        EnvoiResponse envoi,
        List<PieceResponse> pieces
) {
}
