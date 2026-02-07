package io.github.rivon0507.courier.envoi.api;

import java.util.List;

public record EnvoiCreateRequest(
        String reference,
        String observation,
        String dateEnvoi,
        List<Piece> pieces
) {
    public record Piece(
            String designation,
            Integer quantite
    ) {
    }
}
