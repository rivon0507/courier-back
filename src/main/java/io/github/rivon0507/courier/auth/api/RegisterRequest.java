package io.github.rivon0507.courier.auth.api;

import jakarta.validation.constraints.*;

public record RegisterRequest(
        @Email
        @NotBlank
        String email,

        @NotBlank
        String password,

        @Size(min = 1, max = 80)
        @NotNull
        @Pattern(regexp = "^[^\\p{Cntrl}\\n\\r\\t]+$", message = "Display name must not contain line breaks or control characters")
        String displayName
) {
}
