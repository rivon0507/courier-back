package io.github.rivon0507.courier.envoi.api;

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

class EnvoiCreateRequestValidationTest {
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
                new Case("missing dateEnvoi", dtoNoPiece("ref", "obs", null)),
                new Case("missing piece designation", dto("ref", "obs", "2025-12-25", List.of(piece("", 1)))),
                new Case("negative piece quantite", dto("ref", "obs", "2025-12-25", List.of(piece("des", -1)))),
                new Case("piece with zero quantite", dto("ref", "obs", "2025-12-25", List.of(piece("des", 0)))),
                new Case("designation with control characters", dto("ref", "obs", "2025-12-25", List.of(piece("de\0", 2)))),
                new Case("reference 31 characters", dtoNoPiece("r".repeat(31), "obs", "2025-12-25")),
                new Case("observation 61 characters", dtoNoPiece("ref", "o".repeat(61), "2025-12-25"))
        ).map(c -> DynamicTest.dynamicTest(c.name, () -> {
            Validator validator = validatorFactory.getValidator();
            Set<ConstraintViolation<EnvoiCreateRequest>> validate = validator.validate(c.dto);
            assertThat(validate).isNotEmpty();
        }));
    }

    @TestFactory
    Stream<DynamicTest> passing() {
        return Stream.of(
                new Case("null observation", dtoNoPiece("ref", null, "2025-12-25")),
                new Case("empty observation", dtoNoPiece("ref", "", "2025-12-25")),
                new Case("null reference", dtoNoPiece(null, "obs", "2025-12-25")),
                new Case("reference 30 characters", dtoNoPiece("r".repeat(30), "obs", "2025-12-25")),
                new Case("observation 60 characters", dtoNoPiece("ref", "o".repeat(60), "2025-12-25"))
        ).map(c -> DynamicTest.dynamicTest(c.name, () -> {
            Validator validator = validatorFactory.getValidator();
            Set<ConstraintViolation<EnvoiCreateRequest>> validate = validator.validate(c.dto);
            assertThat(validate).isEmpty();
        }));
    }

    private static @NonNull PieceCreateRequest piece(String designation, int quantite) {
        return new PieceCreateRequest(designation, quantite);
    }

    private record Case(String name, EnvoiCreateRequest dto) {
    }

    private @NonNull EnvoiCreateRequest dto(
            String reference,
            String observation,
            @Nullable String dateEnvoi,
            List<PieceCreateRequest> pieces
    ) {
        return new EnvoiCreateRequest(reference, observation, Optional.ofNullable(dateEnvoi).map(LocalDate::parse).orElse(null), pieces);
    }

    private @NonNull EnvoiCreateRequest dtoNoPiece(
            String reference,
            String observation,
            String dateEnvoi
    ) {
        return dto(reference, observation, dateEnvoi, List.of());
    }
}