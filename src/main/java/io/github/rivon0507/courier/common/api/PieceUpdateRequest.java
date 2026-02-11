package io.github.rivon0507.courier.common.api;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record PieceUpdateRequest(
        @NotNull Long id,
        @NotBlank @Pattern(regexp = "^[^\\p{Cntrl}]+$") String designation,
        @Min(1) Integer quantite
) {
}
