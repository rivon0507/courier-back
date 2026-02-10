package io.github.rivon0507.courier.envoi.api;

import java.time.LocalDate;

public record EnvoiUpdateRequest(
        String reference,
        String observation,
        LocalDate dateEnvoi
) {
}
