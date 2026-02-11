package io.github.rivon0507.courier.reception.api;

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
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class ReceptionUpdateRequestValidationTest {

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
                new Case("missing dateReception", dto("RECEP-1", "exp", null)),
                new Case("missing expediteur", dto("REF", null, "2025-12-25")),
                new Case("blank expediteur", dto("RECEP-1", "", "2025-12-25")),
                new Case("reference 31 characters", dto("r".repeat(31), "exp", "2025-12-25")),
                new Case("null reference", dto(null, "exp", "2025-12-05")),
                new Case("blank reference", dto("", "exp", "2025-12-05"))
        ).map(c -> DynamicTest.dynamicTest(c.name, () -> {
            Validator validator = validatorFactory.getValidator();
            Set<ConstraintViolation<ReceptionUpdateRequest>> violations = validator.validate(c.dto);
            assertThat(violations).isNotEmpty();
        }));
    }

    @TestFactory
    Stream<DynamicTest> passing() {
        return Stream.of(
                new Case("normal", dto("REF", "expediteur", "2025-12-25")),
                new Case("reference 30 characters", dto("R".repeat(30), "expediteur", "2025-12-25"))
        ).map(c -> DynamicTest.dynamicTest(c.name, () -> {
            Validator validator = validatorFactory.getValidator();
            Set<ConstraintViolation<ReceptionUpdateRequest>> violations = validator.validate(c.dto);
            assertThat(violations).isEmpty();
        }));
    }

    private record Case(String name, ReceptionUpdateRequest dto) {
    }

    private @NonNull ReceptionUpdateRequest dto(
            String reference,
            String destinataire,
            @Nullable String dateReception
    ) {
        return new ReceptionUpdateRequest(
                reference,
                destinataire,
                Optional.ofNullable(dateReception).map(LocalDate::parse).orElse(null)
        );
    }
}