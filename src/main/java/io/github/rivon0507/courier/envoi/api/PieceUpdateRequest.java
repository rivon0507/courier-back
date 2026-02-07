package io.github.rivon0507.courier.envoi.api;

public record PieceUpdateRequest(
        Long id,
        String designation,
        Integer quantite
) {
}
