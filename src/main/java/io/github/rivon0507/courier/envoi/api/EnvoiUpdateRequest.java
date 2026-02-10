package io.github.rivon0507.courier.envoi.api;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record EnvoiUpdateRequest(
        String reference,
        String observation,
        @NotNull LocalDate dateEnvoi
) {
}
