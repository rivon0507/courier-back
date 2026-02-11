package io.github.rivon0507.courier.reception.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.Length;

import java.time.LocalDate;

public record ReceptionUpdateRequest(
        @Length(max = 30) @NotBlank @Length(max = 30) String reference,
        @NotBlank String expediteur,
        @NotNull LocalDate dateReception
) {
}
