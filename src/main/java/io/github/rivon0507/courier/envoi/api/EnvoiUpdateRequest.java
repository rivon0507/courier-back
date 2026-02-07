package io.github.rivon0507.courier.envoi.api;

public record EnvoiUpdateRequest(
        String reference,
        String observation,
        String dateEnvoi
) {
}
