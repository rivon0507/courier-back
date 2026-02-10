package io.github.rivon0507.courier.envoi.api;

import java.time.LocalDate;

public record EnvoiResponse(
        long id,
        String reference,
        String observation,
        LocalDate dateEnvoi
) {
}
