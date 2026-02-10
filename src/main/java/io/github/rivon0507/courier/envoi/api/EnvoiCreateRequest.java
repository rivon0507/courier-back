package io.github.rivon0507.courier.envoi.api;

import java.time.LocalDate;
import java.util.List;

public record EnvoiCreateRequest(
        String reference,
        String observation,
        LocalDate dateEnvoi,
        List<PieceCreateRequest> pieces
) {
}
