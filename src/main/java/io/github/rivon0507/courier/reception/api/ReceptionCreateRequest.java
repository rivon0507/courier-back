package io.github.rivon0507.courier.reception.api;

import io.github.rivon0507.courier.common.api.PieceCreateRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.Length;

import java.time.LocalDate;
import java.util.List;

public record ReceptionCreateRequest(
        @Length(max = 30) @NotBlank String reference,
        @NotBlank String expediteur,
        @NotNull LocalDate dateReception,
        @Valid List<PieceCreateRequest> pieces
) {
}
