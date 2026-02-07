package io.github.rivon0507.courier.envoi.api;

import java.util.List;
import java.util.Map;

public record EnvoiUpdateRequest(
        String reference,
        String observation,
        String dateEnvoi,
        List<Piece> createPieces,
        Map<Long, Piece> updatePieces,
        List<Long> deletePieces
) {
    public record Piece(
            String designation,
            String quantite
    ) {
    }
}
