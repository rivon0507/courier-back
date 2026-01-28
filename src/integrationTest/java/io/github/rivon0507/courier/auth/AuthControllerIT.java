package io.github.rivon0507.courier.auth;

import io.github.rivon0507.courier.IntegrationTest;
import io.github.rivon0507.courier.auth.domain.RefreshToken;
import io.github.rivon0507.courier.auth.domain.RefreshTokenRepository;
import io.github.rivon0507.courier.auth.service.RefreshTokenHasher;
import io.github.rivon0507.courier.common.domain.Role;
import io.github.rivon0507.courier.common.domain.User;
import io.github.rivon0507.courier.common.persistence.UserRepository;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import static org.junit.jupiter.api.DynamicTest.dynamicTest;

@IntegrationTest
public class AuthControllerIT {
    @Autowired
    private RestTestClient restClient;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @LocalServerPort
    int serverPort;

    @Contract("_, _ -> new")
    private static AuthControllerIT.Case caseOf(String name, @Language("JSON") String body) {
        return new Case(name, body);
    }

    @BeforeEach
    void setUp() {
        var user = new User();
        user.setEmail("user@example.com");
        user.setDisplayName("User");
        user.setPasswordHash(passwordEncoder.encode("password"));
        user.setActive(true);
        user.setRole(Role.USER);
        userRepository.save(user);

        var inactiveUser = new User();
        inactiveUser.setEmail("inactive@example.com");
        inactiveUser.setDisplayName("Inactive");
        inactiveUser.setPasswordHash(passwordEncoder.encode("password"));
        inactiveUser.setActive(false);
        inactiveUser.setRole(Role.USER);
        userRepository.save(inactiveUser);
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.execute("TRUNCATE users RESTART IDENTITY CASCADE");
    }

    private String assertAuthResponseAndExtractToken(RestTestClient.@NonNull BodyContentSpec body,
                                                     String expectedEmail,
                                                     String expectedDisplayName) {
        AtomicReference<String> tokenRef = new AtomicReference<>();

        body.jsonPath("$.accessToken").isNotEmpty()
                .jsonPath("$.expiresIn").isNumber()
                .jsonPath("$.tokenType").isEqualTo("Bearer")
                .jsonPath("$.user.displayName").isEqualTo(expectedDisplayName)
                .jsonPath("$.user.email").isEqualTo(expectedEmail)
                .jsonPath("$.user.role").isEqualTo("USER")
                .jsonPath("$.accessToken").value(v -> tokenRef.set(String.valueOf(v)));

        return tokenRef.get();
    }

    private void assertTokenWorks(String accessToken) {
        restClient.get().uri("/_security/ping")
                .header("Authorization", "Bearer " + accessToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).isEqualTo("pong");
    }

    private record Case(String name, @Language("JSON") String body) {
    }

    @Nested
    class LoginTests {

        @TestFactory
        Stream<DynamicTest> invalidLoginRequest__returns400() {

            return Stream.of(
                    caseOf("missing email", "{\"password\": \"password\"}"),
                    caseOf("missing password", "{\"email\": \"email\"}"),
                    caseOf("blank email", "{\"email\": \"\",\"password\": \"password\"}"),
                    caseOf("blank password", "{\"email\": \"email\", \"password\": \"\"}"),
                    caseOf("no request body", null)
            ).map(c ->
                    dynamicTest(c.name(), () -> {
                        var request = restClient.post().uri("/auth/login");
                        if (c.body() != null) request.body(c.body());
                        request.exchange().expectStatus().isBadRequest();
                    })
            );
        }

        @TestFactory
        Stream<DynamicTest> invalidCredentials__returns401() {
            return Stream.of(
                    caseOf("nonexistent email", "{\"email\": \"nonexistent@example.com\", \"password\": \"password\"}"),
                    caseOf("invalid password", "{\"email\": \"user@example.com\", \"password\": \"invalid password\"}"),
                    caseOf("deactivated user", "{\"email\": \"inactive@example.com\", \"password\": \"password\"}")
            ).map(c ->
                    dynamicTest(c.name(), () -> restClient
                            .post().uri("/auth/login")

                            .body(c.body())
                            .exchange().expectStatus().isUnauthorized()
                    )
            );
        }

        @Test
        void validCredentials__returns200WithUsableToken() {
            var body = restClient.post()
                    .uri("/auth/login")
                    .body("{\"email\":\"user@example.com\",\"password\":\"password\"}")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody();
            String accessToken = assertAuthResponseAndExtractToken(body, "user@example.com", "User");
            assertTokenWorks(accessToken);
        }
    }

    @Nested
    class RegisterTests {
        @TestFactory
        Stream<DynamicTest> invalidRequestBody__returns400() {
            return Stream.of(
                    caseOf("missing email", "{\"password\": \"password\", \"displayName\": \"New User\"}"),
                    caseOf("invalid email", "{\"email\": \"invalid\", \"password\": \"password\", \"displayName\": \"User\"}"),
                    caseOf("blank email", "{\"email\": \"\", \"password\": \"password\", \"displayName\": \"User\"}"),
                    caseOf("missing password", "{\"email\": \"user@example.com\", \"displayName\": \"User\"}"),
                    caseOf("blank password", "{\"email\": \"user@example.com\", \"password\": \"\", \"displayName\": \"User\"}"),
                    caseOf("missing displayName", "{\"email\": \"user@example.com\", \"password\": \"password\"}"),
                    caseOf("invalid displayName", "{\"email\": \"user@example.com\", \"password\": \"password\", \"displayName\": \"\\r\"}"),
                    caseOf("displayName too long", "{\"email\": \"user@example.com\", \"password\": \"password\", \"displayName\": \"%s\"}".formatted("a".repeat(81))),
                    caseOf("blank displayName", "{\"email\": \"user@example.com\", \"password\": \"password\", \"displayName\": \"\"}"),
                    caseOf("no request body", null)
            ).map(c -> dynamicTest(c.name, () -> {
                var requestBodySpec = restClient.post().uri("/auth/register");
                if (c.body != null) requestBodySpec.body(c.body);
                requestBodySpec.exchange().expectStatus().isBadRequest();
            }));
        }

        @TestFactory
        Stream<DynamicTest> withExistingEmail__returns409() {
            return Stream.of(
                    dynamicTest("register twice", () -> {
                        String requestBody = "{\"email\": \"newuser@example.com\", \"password\": \"password\", \"displayName\": \"User\"}";
                        restClient.post().uri("/auth/register")
                                .body(requestBody)
                                .exchangeSuccessfully();
                        restClient.post().uri("/auth/register")
                                .body(requestBody)
                                .exchange()
                                .expectStatus().isEqualTo(HttpStatus.CONFLICT)
                                .expectBody()
                                .jsonPath("$.code").isEqualTo("EMAIL_ALREADY_TAKEN");
                    }),
                    dynamicTest("register with the email of an inactive user", () -> restClient.post()
                            .uri("/auth/register")
                            .body("{\"email\": \"inactive@example.com\", \"password\": \"password\", \"displayName\": \"User\"}")
                            .exchange()
                            .expectStatus().isEqualTo(HttpStatus.CONFLICT)
                            .expectBody()
                            .jsonPath("$.code").isEqualTo("EMAIL_ALREADY_TAKEN")
                    )
            );
        }

        @Test
        void successfulRegister__returns201() {
            var body = restClient.post().uri("/auth/register")
                    .body("{\"email\": \"newuser@example.com\", \"password\": \"password\", \"displayName\": \"New\"}")
                    .exchange()
                    .expectStatus().isCreated()
                    .expectHeader().location("http://localhost:%d/api/users/me".formatted(serverPort))
                    .expectBody();

            String accessToken = assertAuthResponseAndExtractToken(body, "newuser@example.com", "New");
            assertTokenWorks(accessToken);
        }
    }
}