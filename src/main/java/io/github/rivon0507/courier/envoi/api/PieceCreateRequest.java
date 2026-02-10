package io.github.rivon0507.courier.envoi.api;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record PieceCreateRequest(
        @NotBlank @Pattern(regexp = "^[^\\p{Cntrl}]+$") String designation,
        @Min(1) Integer quantite
) {
}
