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
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class EnvoiUpdateRequestValidationTest {
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
                new Case("missing dateEnvoi", dto("ref", "obs", null)),
                new Case("designation with control characters", dto("ref", "obs", "2025-12-25")),
                new Case("reference 31 characters", dto("r".repeat(31), "obs", "2025-12-25")),
                new Case("observation 61 characters", dto("ref", "o".repeat(61), "2025-12-25"))
        ).map(c -> DynamicTest.dynamicTest(c.name, () -> {
            Validator validator = validatorFactory.getValidator();
            Set<ConstraintViolation<EnvoiUpdateRequest>> validate = validator.validate(c.dto);
            assertThat(validate).isNotEmpty();
        }));
    }

    @TestFactory
    Stream<DynamicTest> passing() {
        return Stream.of(
                new Case("null observation", dto("ref", null, "2025-12-25")),
                new Case("empty observation", dto("ref", "", "2025-12-25")),
                new Case("null reference", dto(null, "obs", "2025-12-25")),
                new Case("reference 30 characters", dto("r".repeat(30), "obs", "2025-12-25")),
                new Case("observation 60 characters", dto("ref", "o".repeat(60), "2025-12-25"))
        ).map(c -> DynamicTest.dynamicTest(c.name, () -> {
            Validator validator = validatorFactory.getValidator();
            Set<ConstraintViolation<EnvoiUpdateRequest>> validate = validator.validate(c.dto);
            assertThat(validate).isEmpty();
        }));
    }

    private record Case(String name, EnvoiUpdateRequest dto) {
    }

    private @NonNull EnvoiUpdateRequest dto(
            String reference,
            String observation,
            @Nullable String dateEnvoi
    ) {
        return new EnvoiUpdateRequest(reference, observation, Optional.ofNullable(dateEnvoi).map(LocalDate::parse).orElse(null));
    }
}