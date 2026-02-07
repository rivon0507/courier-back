package io.github.rivon0507.courier.envoi.api;

public record PieceCreateRequest(
        String designation,
        Integer quantite
) {
}
