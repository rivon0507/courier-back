package io.github.rivon0507.courier.reception.api;

import io.github.rivon0507.courier.common.api.PieceCreateRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class ReceptionCreateRequestValidationTest {

    private static ValidatorFactory validatorFactory;

    @BeforeAll
    static void beforeAll() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
    }

    @AfterAll
    static void afterAll() {
        validatorFactory.close();
    }

    @TestFactory
    Stream<DynamicTest> failing() {
        return Stream.of(
                new Case("missing dateReception", dtoNoPiece("RECEP-1", "exp", null)),
                new Case("missing expediteur", dtoNoPiece("REF", null, "2025-12-25")),
                new Case("blank expediteur", dtoNoPiece("RECEP-1", "", "2025-12-25")),
                new Case("reference 31 characters", dtoNoPiece("r".repeat(31), "exp", "2025-12-25")),
                new Case("null reference", dtoNoPiece(null, "exp", "2025-12-05")),
                new Case("blank reference", dtoNoPiece("", "exp", "2025-12-05")),
                new Case("blank piece designation", dto("ref", "dest", "2025-12-25", List.of(piece("", 1)))),
                new Case("negative piece quantite", dto("ref", "dest", "2025-12-25", List.of(piece("des", -1)))),
                new Case("piece with zero quantite", dto("ref", "dest", "2025-12-25", List.of(piece("des", 0)))),
                new Case("piece designation with control characters", dto("ref", "dest", "2025-12-25", List.of(piece("de\0", 2))))
        ).map(c -> DynamicTest.dynamicTest(c.name, () -> {
            Validator validator = validatorFactory.getValidator();
            Set<ConstraintViolation<ReceptionCreateRequest>> violations = validator.validate(c.dto);
            assertThat(violations).isNotEmpty();
        }));
    }

    @TestFactory
    Stream<DynamicTest> passing() {
        return Stream.of(
                new Case("no piece", dtoNoPiece("REF", "expediteur", "2025-12-25")),
                new Case("reference 30 characters", dtoNoPiece("R".repeat(30), "expediteur", "2025-12-25")),
                new Case("with 1 piece", dto("R", "e", "2025-12-25", List.of(piece("d", 10))))
        ).map(c -> DynamicTest.dynamicTest(c.name, () -> {
            Validator validator = validatorFactory.getValidator();
            Set<ConstraintViolation<ReceptionCreateRequest>> violations = validator.validate(c.dto);
            assertThat(violations).isEmpty();
        }));
    }

    private static @NonNull PieceCreateRequest piece(String designation, int quantite) {
        return new PieceCreateRequest(designation, quantite);
    }

    private record Case(String name, ReceptionCreateRequest dto) {
    }

    private @NonNull ReceptionCreateRequest dto(
            String reference,
            String expediteur,
            @Nullable String dateReception,
            List<PieceCreateRequest> pieces
    ) {
        return new ReceptionCreateRequest(
                reference,
                expediteur,
                Optional.ofNullable(dateReception).map(LocalDate::parse).orElse(null),
                pieces
        );
    }

    private @NonNull ReceptionCreateRequest dtoNoPiece(
            String reference,
            String expediteur,
            String dateReception
    ) {
        return dto(reference, expediteur, dateReception, List.of());
    }
}