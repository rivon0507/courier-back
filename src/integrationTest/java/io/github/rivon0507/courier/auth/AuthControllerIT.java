package io.github.rivon0507.courier.auth;

import io.github.rivon0507.courier.IntegrationTest;
import io.github.rivon0507.courier.common.domain.Role;
import io.github.rivon0507.courier.common.domain.User;
import io.github.rivon0507.courier.common.persistence.UserRepository;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.util.stream.Stream;

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
                    DynamicTest.dynamicTest(c.name(), () -> {
                        var request = restClient.post().uri("/auth/login");
                        if (c.body() != null) request.body(c.body());
                        request.contentType(MediaType.APPLICATION_JSON).exchange().expectStatus().isBadRequest();
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
                    DynamicTest.dynamicTest(c.name(), () -> restClient
                            .post().uri("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(c.body())
                            .exchange().expectStatus().isUnauthorized()
                    )
            );
        }

        @Test
        void validCredentials__returns200WithUsableToken() {
            final String[] accessToken = new String[1];
            restClient.post()
                    .uri("/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"email\":\"user@example.com\",\"password\":\"password\"}")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.accessToken").isNotEmpty()
                    .jsonPath("$.expiresIn").isNumber()
                    .jsonPath("$.tokenType").isEqualTo("Bearer")
                    .jsonPath("$.user.displayName").isEqualTo("User")
                    .jsonPath("$.user.email").isEqualTo("user@example.com")
                    .jsonPath("$.user.role").isEqualTo("USER")
                    .jsonPath("$.accessToken").value(o -> accessToken[0] = o.toString());

            restClient.get().uri("/_security/ping")
                    .header("Authorization", "Bearer " + accessToken[0])
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody().jsonPath("$").isEqualTo("pong");
        }

        private record Case(String name, @Language("JSON") String body) {
        }

        @Contract("_, _ -> new")
        private static @NonNull Case caseOf(String name, @Language("JSON") String body) {
            return new Case(name, body);
        }
    }

}