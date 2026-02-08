package io.github.rivon0507.courier.envoi.api;

public record EnvoiResponse(
        long id,
        String reference,
        String observation,
        String dateEnvoi
) {
}
