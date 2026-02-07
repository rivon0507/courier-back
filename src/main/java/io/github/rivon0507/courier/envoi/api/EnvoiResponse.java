package io.github.rivon0507.courier.envoi.api;

import java.util.List;

public record EnvoiResponse(
        long id,
        String reference,
        String observation,
        String dateEnvoi,
        List<Piece> pieces
) {
    public record Piece(
            long id,
            String designation,
            int quantite
    ) {
    }
}
