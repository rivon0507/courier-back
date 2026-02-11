package io.github.rivon0507.courier.reception.api;

import java.time.LocalDate;

public record ReceptionResponse(
        long id,
        String reference,
        String expediteur,
        LocalDate dateReception
) {
}
