package io.github.rivon0507.courier.envoi.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.Length;

import java.time.LocalDate;
import java.util.List;

public record EnvoiCreateRequest(
        @Length(max = 30) String reference,
        @Length(max = 60) String observation,
        @NotNull LocalDate dateEnvoi,
        @Valid List<PieceCreateRequest> pieces
) {
}
