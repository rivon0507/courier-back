package io.github.rivon0507.courier.envoi.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.Length;

import java.time.LocalDate;

public record EnvoiUpdateRequest(
        @Length(max = 30) String reference,
        @NotBlank String destinataire,
        @Length(max = 60) String observation,
        @NotNull LocalDate dateEnvoi
) {
}
