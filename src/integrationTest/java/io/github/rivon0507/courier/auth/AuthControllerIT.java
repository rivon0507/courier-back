package io.github.rivon0507.courier.auth;

import io.github.rivon0507.courier.IntegrationTest;
import io.github.rivon0507.courier.auth.domain.RefreshToken;
import io.github.rivon0507.courier.auth.domain.RefreshTokenRepository;
import io.github.rivon0507.courier.auth.service.RefreshTokenHasher;
import io.github.rivon0507.courier.common.domain.Role;
import io.github.rivon0507.courier.common.domain.User;
import io.github.rivon0507.courier.common.persistence.UserRepository;
import org.apache.hc.client5.http.cookie.BasicCookieStore;
import org.apache.hc.client5.http.cookie.Cookie;
import org.apache.hc.client5.http.cookie.CookieStore;
import org.apache.hc.client5.http.impl.cookie.BasicClientCookie;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatList;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

@IntegrationTest
public class AuthControllerIT {
    private static final String VALID_DEVICE_ID = "693e5f1b-914b-49b7-8362-8855de4a5cf9";
    @LocalServerPort
    int serverPort;
    @Autowired
    private RestTestClient restClient;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private RefreshTokenRepository refreshTokenRepository;
    @Autowired
    private RefreshTokenHasher refreshTokenHasher;
    @Autowired
    private CookieStore cookieStore;
    @Autowired
    private TransactionTemplate transactionTemplate;
    @Autowired
    private Function<CookieStore, RestTestClient> restTestClientFactory;

    @Contract("_, _ -> new")
    private static AuthControllerIT.Case caseOf(String name, @Language("JSON") String body) {
        return new Case(name, body);
    }

    @BeforeEach
    void setUp() {
        cookieStore.clear();
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
        jdbcTemplate.execute("TRUNCATE users, refresh_tokens RESTART IDENTITY CASCADE");
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

    private void assertCookieStoreDoesNotContain(String cookieName) {
        String description = "The %s cookie should have been cleared".formatted(cookieName);
        assertThat(cookieStore.getCookies())
                .as(description)
                .filteredOn(c -> c.getName().equals(cookieName))
                .isEmpty();
    }

    private RefreshToken findTokenByHash(String token) {
        byte[] hash = refreshTokenHasher.hash(token);
        return transactionTemplate.execute(status -> refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new AssertionError("Expected token not found in repository")));
    }

    @SuppressWarnings("SameParameterValue")
    private AuthResult refreshSession(String email, String displayName) {
        AtomicReference<String> refreshToken = new AtomicReference<>();
        var body = restClient.post().uri("/auth/refresh")

                .exchange().expectStatus().isOk()
                .expectCookie().exists("refresh_token")
                .expectCookie().value("refresh_token", refreshToken::set)
                .expectBody();
        String accessToken = assertAuthResponseAndExtractToken(body, email, displayName);
        return new AuthResult(refreshToken.get(), accessToken);
    }

    @SuppressWarnings("SameParameterValue")
    private AuthResult login(String email, String password, String displayName) {
        return login(restClient, email, password, displayName);
    }

    private AuthResult login(RestTestClient client, String email, String password, String displayName) {
        AtomicReference<String> refreshToken = new AtomicReference<>();
        var body = client.post().uri("/auth/login")

                .body(Map.of("email", email, "password", password))
                .exchange().expectStatus().isOk()
                .expectCookie().exists("refresh_token")
                .expectCookie().value("refresh_token", refreshToken::set)
                .expectBody();
        String accessToken = assertAuthResponseAndExtractToken(body, email, displayName);
        return new AuthResult(refreshToken.get(), accessToken);
    }

    private void logout() {
        logout(restClient);
    }

    private void logout(RestTestClient client) {
        client.post().uri("/auth/logout")
                .exchange()
                .expectStatus().isNoContent();
        assertCookieStoreDoesNotContain("refresh_token");
    }

    private @NonNull BasicClientCookie getDeviceIdCookieFromStore() {
        return (BasicClientCookie) cookieStore.getCookies().stream()
                .filter(c -> c.getName().equals("device_id"))
                .findFirst().orElseThrow();
    }

    private void removeCookie(String name) {
        setCookie(name, "", Instant.now().minus(Duration.ofHours(1)));
    }

    private void setCookie(String name, String value) {
        setCookie(name, value, Instant.now().plus(Duration.ofDays(7)));
    }

    private void setCookie(String name, String value, Instant expiryDate) {
        BasicClientCookie cookie = new BasicClientCookie(name, value);
        cookie.setExpiryDate(expiryDate);
        cookie.setPath("/");
        cookie.setDomain("localhost");
        cookieStore.addCookie(cookie);
    }

    private void assertCookieStoreContainsDeviceId() {
        assertThat(cookieStore.getCookies())
                .as("The device_id cookie should not be cleared")
                .filteredOn(c -> c.getName().equals("device_id"))
                .isNotEmpty();
    }

    private record Case(String name, @Language("JSON") String body) {
    }

    private record AuthResult(String refreshToken, String accessToken) {
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

        @Test
        void login_after_logout_returns_refresh_token_with_different_familyId() {
            String token1 = login("user@example.com", "password", "User").refreshToken;
            logout();
            String token2 = login("user@example.com", "password", "User").refreshToken;
            RefreshToken session1 = findTokenByHash(token1);
            RefreshToken session2 = findTokenByHash(token2);

            assertThat(session1.getFamilyId()).isNotEqualTo(session2.getFamilyId());
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

    @Nested
    class RefreshSessionTests {
        @Test
        void refresh_with_malformed_device_id_returns_401_and_clears_both_cookies() {
            login("user@example.com", "password", "User");
            setCookie("device_id", "malformed");

            restClient.post().uri("/auth/refresh")
                    .exchange()
                    .expectStatus().isUnauthorized()
                    .expectBody()
                    .jsonPath("$.code").isEqualTo("INVALID_SESSION");

            assertCookieStoreDoesNotContain("refresh_token");
            assertCookieStoreDoesNotContain("device_id");
        }

        @Nested
        class HappyPath {

            @Test
            void login_then_refresh_rotates_once() {
                AuthResult loginResult = login("user@example.com", "password", "User");
                String loginDeviceId = getDeviceIdCookieFromStore().getValue();
                AuthResult refreshResult = refreshSession("user@example.com", "User");
                String refreshDeviceId = getDeviceIdCookieFromStore().getValue();

                assertThat(refreshDeviceId).isEqualTo(loginDeviceId);
                assertThat(refreshResult.refreshToken).isNotEqualTo(loginResult.refreshToken);
                assertTokenWorks(refreshResult.accessToken);

                RefreshToken oldToken = findTokenByHash(loginResult.refreshToken);
                RefreshToken newToken = findTokenByHash(refreshResult.refreshToken);
                Instant now = Instant.now();

                assertThat(oldToken.wasRotated()).isTrue();
                assertThat(oldToken.wasReused()).isFalse();
                assertThat(newToken.isActive(now)).isTrue();
                assertThat(oldToken.getReplacedByTokenId()).isEqualTo(newToken.getId());
                assertThat(oldToken.getFamilyId()).isEqualTo(newToken.getFamilyId());
            }

            @Test
            void login_then_refresh_twice_builds_rotation_chain() {
                AuthResult loginResult = login("user@example.com", "password", "User");
                AuthResult refresh1 = refreshSession("user@example.com", "User");
                AuthResult refresh2 = refreshSession("user@example.com", "User");

                assertThat(refresh2.refreshToken).isNotEqualTo(loginResult.refreshToken);
                assertThat(refresh2.refreshToken).isNotEqualTo(refresh1.refreshToken);
                assertTokenWorks(refresh2.accessToken);

                RefreshToken loginToken = findTokenByHash(loginResult.refreshToken);
                RefreshToken refreshToken1 = findTokenByHash(refresh1.refreshToken);
                RefreshToken refreshToken2 = findTokenByHash(refresh2.refreshToken);
                Instant now = Instant.now();

                assertThat(refreshToken1.wasRotated()).isTrue();
                assertThat(refreshToken2.isActive(now)).isTrue();
                assertThat(loginToken.getReplacedByTokenId()).isEqualTo(refreshToken1.getId());
                assertThat(refreshToken1.getReplacedByTokenId()).isEqualTo(refreshToken2.getId());
                assertThat(List.of(loginToken, refreshToken1, refreshToken2))
                        .extracting(RefreshToken::getFamilyId)
                        .containsOnly(loginToken.getFamilyId());
            }
        }

        @Nested
        class ReuseDetection {

            @Test
            void reusing_rotated_token_revokes_family_and_blocks_latest() {
                AuthResult loginResult = login("user@example.com", "password", "User");
                AuthResult refreshResult = refreshSession("user@example.com", "User");
                setCookie("refresh_token", loginResult.refreshToken);
                // Refresh with the old token -> reuse detected
                restClient.post().uri("/auth/refresh")
                        .exchange()
                        .expectStatus().isUnauthorized()
                        .expectBody()
                        .jsonPath("$.code").isEqualTo("REFRESH_TOKEN_REUSED");
                assertCookieStoreDoesNotContain("refresh_token");

                setCookie("refresh_token", refreshResult.refreshToken);
                // Refresh with the most recent token -> session killed because reuse was detected
                restClient.post().uri("/auth/refresh")
                        .exchange()
                        .expectStatus().isUnauthorized()
                        .expectBody()
                        .jsonPath("$.code").isEqualTo("REFRESH_TOKEN_REUSED");
                assertCookieStoreDoesNotContain("refresh_token");

                RefreshToken refreshedSession = findTokenByHash(refreshResult.refreshToken);
                assertThat(refreshTokenRepository.findAllByFamilyId(refreshedSession.getFamilyId()))
                        .as("No new token should have been issued.")
                        .hasSize(2);
                assertThat(refreshedSession.wasReused())
                        .as("The most recent token should be marked as reused")
                        .isTrue();
            }
        }

        @Nested
        class Failures {

            @ParameterizedTest(name = "[{0}]: {1}")
            @CsvSource(value = {"missing_token, null", "invalid_token, invalid_token"}, nullValues = "null")
            void refresh_without_device_id_cookie_returns_401(String ignoredTitle, @Nullable String refreshToken) {
                setCookie("refresh_token", refreshToken);
                restClient.post().uri("/auth/refresh")
                        .exchange()
                        .expectStatus().isUnauthorized()
                        .expectBody()
                        .jsonPath("$.code").isEqualTo("INVALID_SESSION");

                assertCookieStoreDoesNotContain("refresh_token");
            }

            @Test
            void refresh_with_invalid_token_cookie_returns_401() {
                setCookie("device_id", VALID_DEVICE_ID);
                setCookie("refresh_token", "invalid_token");
                restClient.post().uri("/auth/refresh")
                        .exchange()
                        .expectStatus().isUnauthorized()
                        .expectBody()
                        .jsonPath("$.code").isEqualTo("INVALID_SESSION");

                assertCookieStoreDoesNotContain("refresh_token");
            }

            @Test
            void refresh_with_missing_token_cookie_returns_401() {
                setCookie("device_id", VALID_DEVICE_ID);
                restClient.post().uri("/auth/refresh")
                        .exchange()
                        .expectStatus().isUnauthorized()
                        .expectBody()
                        .jsonPath("$.code").isEqualTo("INVALID_SESSION");

                assertCookieStoreDoesNotContain("refresh_token");
            }

            @Test
            void refresh_with_expired_refresh_token_returns_401_without_rotation() {
                String loginToken = login("user@example.com", "password", "User").refreshToken;
                RefreshToken loginSession = findTokenByHash(loginToken);
                loginSession.setExpiresAt(Instant.now().minus(Duration.ofMinutes(5)));
                refreshTokenRepository.save(loginSession);

                restClient.post().uri("/auth/refresh")
                        .exchange()
                        .expectStatus().isUnauthorized()
                        .expectBody()
                        .jsonPath("$.code").isEqualTo("INVALID_SESSION");

                assertCookieStoreDoesNotContain("refresh_token");
                assertThat(refreshTokenRepository.count()).isEqualTo(1);
                assertThat(findTokenByHash(loginToken).wasRotated())
                        .as("The token should not be rotated")
                        .isFalse();
            }

            @Test
            void refresh_after_logout_returns_401() {
                login("user@example.com", "password", "User");
                logout();
                restClient.post().uri("/auth/refresh")
                        .exchange()
                        .expectStatus().isUnauthorized()
                        .expectBody()
                        .jsonPath("$.code").isEqualTo("INVALID_SESSION");
                assertCookieStoreDoesNotContain("refresh_token");
            }
        }
    }

    @Nested
    class DevicePartitionTests {

        @Test
        void device_id_cookie_is_stable() {
            Set<String> deviceIds = new HashSet<>();
            login("user@example.com", "password", "User");
            deviceIds.add(getDeviceIdCookieFromStore().getValue());
            assertThat(deviceIds).hasSize(1);
            refreshSession("user@example.com", "User");
            deviceIds.add(getDeviceIdCookieFromStore().getValue());
            assertThat(deviceIds).hasSize(1);
            restClient.post().uri("/auth/register")
                    .body("{\"email\": \"newuser@example.com\", \"password\": \"password\", \"displayName\": \"New\"}")
                    .exchangeSuccessfully();
            deviceIds.add(getDeviceIdCookieFromStore().getValue());
            assertThat(deviceIds).hasSize(1);
            login("newuser@example.com", "password", "New");
            deviceIds.add(getDeviceIdCookieFromStore().getValue());
            assertThat(deviceIds).hasSize(1);
        }

        @Test
        void login_twice_with_same_device_id_replaces_previous_session() {
            AuthResult login1 = login("user@example.com", "password", "User");
            AuthResult login2 = login("user@example.com", "password", "User");
            RefreshToken loginSession1 = findTokenByHash(login1.refreshToken);
            RefreshToken loginSession2 = findTokenByHash(login2.refreshToken);

            assertThat(loginSession1.getFamilyId())
                    .as("The two sessions should have different familyId")
                    .isNotEqualTo(loginSession2.getFamilyId());
            assertThat(loginSession1.getDeviceId())
                    .as("The two sessions should have the same deviceId")
                    .isEqualTo(loginSession2.getDeviceId());
            assertThat(loginSession1.wasLoggedOut())
                    .as("The old session should be logged out")
                    .isTrue();
        }

        @Test
        void register_then_login_with_same_device_id_replaces_previous_session() {
            AtomicReference<String> registerRefreshTokenRef = new AtomicReference<>();
            restClient.post().uri("/auth/register")
                    .body("{\"email\": \"newuser@example.com\", \"password\": \"password\", \"displayName\": \"New\"}")
                    .exchangeSuccessfully()
                    .expectCookie().value("refresh_token", registerRefreshTokenRef::set);
            String loginRefreshToken = login("newuser@example.com", "password", "New").refreshToken;
            RefreshToken registerSession = findTokenByHash(registerRefreshTokenRef.get());
            RefreshToken loginSession = findTokenByHash(loginRefreshToken);

            assertThat(registerSession.getFamilyId())
                    .as("The two sessions should have different familyId")
                    .isNotEqualTo(loginSession.getFamilyId());
            assertThat(registerSession.getDeviceId())
                    .as("The two sessions should have the same deviceId")
                    .isEqualTo(loginSession.getDeviceId());
            assertThat(registerSession.wasLoggedOut())
                    .as("The old session should be logged out")
                    .isTrue();
        }

        @Test
        void login_again_after_removing_device_id_creates_new_family_and_device_id() {
            AuthResult login1 = login("user@example.com", "password", "User");
            // Expire the device_id cookie
            BasicClientCookie deviceIdCookie = getDeviceIdCookieFromStore();
            deviceIdCookie.setExpiryDate(Instant.now().minusSeconds(900));
            cookieStore.addCookie(deviceIdCookie);
            // Re-login
            AuthResult login2 = login("user@example.com", "password", "User");

            RefreshToken loginSession1 = findTokenByHash(login1.refreshToken);
            RefreshToken loginSession2 = findTokenByHash(login2.refreshToken);
            Instant now = Instant.now();

            assertThat(loginSession1.getFamilyId())
                    .as("The two sessions should have different familyId")
                    .isNotEqualTo(loginSession2.getFamilyId());
            assertThat(loginSession1.getDeviceId())
                    .as("The two sessions should have different deviceId")
                    .isNotEqualTo(loginSession2.getDeviceId());
            assertThatList(List.of(loginSession1, loginSession2))
                    .as("The both sessions should be active")
                    .extracting(rt -> rt.isActive(now))
                    .allMatch(b -> b.equals(true));
        }

        @Test
        void login_as_another_user_with_same_device_id_replaces_previous_user_session() {
            AtomicReference<String> registerRefreshTokenRef = new AtomicReference<>();
            restClient.post().uri("/auth/register")
                    .body("{\"email\": \"newuser@example.com\", \"password\": \"password\", \"displayName\": \"New\"}")
                    .exchangeSuccessfully()
                    .expectCookie().value("refresh_token", registerRefreshTokenRef::set);
            String otherUserSessionToken = login("user@example.com", "password", "User").refreshToken;

            RefreshToken registerSession = findTokenByHash(registerRefreshTokenRef.get());
            RefreshToken otherUserLoginSession = findTokenByHash(otherUserSessionToken);

            assertThat(registerSession.wasLoggedOut())
                    .as("The previous user's session should be logged out")
                    .isTrue();
            assertThat(registerSession.getFamilyId())
                    .as("The two sessions should have different familyId")
                    .isNotEqualTo(otherUserLoginSession.getFamilyId());
            assertThat(registerSession.getDeviceId())
                    .as("The two sessions should have the same deviceId")
                    .isEqualTo(otherUserLoginSession.getDeviceId());
            assertThat(registerSession.getUserId())
                    .as("The two sessions should belong to different users")
                    .isNotEqualTo(otherUserLoginSession.getUserId());
        }

        @Test
        void login_with_malformed_device_id_succeeds_but_replaces_the_cookie() {
            setCookie("device_id", "malformed");
            login("user@example.com", "password", "User");

            assertThat(cookieStore.getCookies())
                    .as("The device_id cookie should have changed")
                    .filteredOn(c -> c.getName().equals("device_id"))
                    .isNotEmpty()
                    .first()
                    .extracting(Cookie::getValue)
                    .isNotEqualTo("malformed");
        }

        @Test
        void register_with_malformed_device_id_succeeds_but_replaces_the_cookie() {
            setCookie("device_id", "malformed");
            restClient.post().uri("/auth/register")
                    .body("{\"email\": \"newuser@example.com\", \"password\": \"password\", \"displayName\": \"New\"}")
                    .exchangeSuccessfully();

            assertThat(cookieStore.getCookies())
                    .as("The device_id cookie should have changed")
                    .filteredOn(c -> c.getName().equals("device_id"))
                    .isNotEmpty().first()
                    .extracting(Cookie::getValue)
                    .isNotEqualTo("malformed");
        }

        @Test
        void logout_is_device_scoped_deviceA_logged_out_deviceB_still_valid() {
            // device A = default test client + default cookie store
            RestTestClient deviceA = restClient;
            // device B = new client with its own cookie jar
            CookieStore deviceBStore = new BasicCookieStore();
            RestTestClient deviceB = restTestClientFactory.apply(deviceBStore);

            login(deviceA, "user@example.com", "password", "User");
            login(deviceB, "user@example.com", "password", "User");
            logout(deviceA);

            deviceA.post().uri("/auth/refresh")
                    .exchange()
                    .expectStatus().isUnauthorized()
                    .expectBody()
                    .jsonPath("$.code").isEqualTo("INVALID_SESSION");

            deviceB.post().uri("/auth/refresh")
                    .exchangeSuccessfully();
        }
    }

    /**
     * Logout should ALWAYS return 204
     */
    @Nested
    class LogoutTests {
        @TestFactory
        Stream<DynamicTest> when_missing_one_or_both_cookies_should_not_revoke_anything() {
            return Map.of(
                            "no device_id", List.of("device_id"),
                            "no refresh_token", List.of("refresh_token"),
                            "missing both cookies", List.of("device_id", "refresh_token")
                    )
                    .entrySet().stream()
                    .map(entry -> dynamicTest(entry.getKey(), () -> {

                        String token = login("user@example.com", "password", "User").refreshToken;
                        entry.getValue().forEach(AuthControllerIT.this::removeCookie);
                        logout();
                        assertThat(findTokenByHash(token).isActive(Instant.now()))
                                .as("The token should be active")
                                .isTrue();
                    }));
        }

        @Test
        void with_malformed_device_id_should_not_revoke_anything() {
            String token = login("user@example.com", "password", "User").refreshToken;
            setCookie("device_id", "malformed");
            logout();
            assertThat(findTokenByHash(token).isActive(Instant.now()))
                    .as("The token should be active")
                    .isTrue();
            assertCookieStoreContainsDeviceId();
        }

        @Test
        void with_nonexistent_refresh_token_should_not_revoke_anything() {
            String token = login("user@example.com", "password", "User").refreshToken;
            setCookie("refresh_token", "nonexistent");
            logout();
            assertThat(findTokenByHash(token).isActive(Instant.now()))
                    .as("The token should be active")
                    .isTrue();
            assertCookieStoreContainsDeviceId();
        }

        @Test
        void successful_revokes_the_refresh_token() {
            String token = login("user@example.com", "password", "User").refreshToken;
            logout();

            assertThat(refreshTokenRepository.count()).isOne();
            assertThat(findTokenByHash(token))
                    .as("The token should be revoked with reason logout")
                    .matches(rt -> rt.getRevokedAt() != null && rt.wasLoggedOut());
            assertCookieStoreContainsDeviceId();
        }

        @Test
        void concurrent_logout_revokes_token_and_returns_204() throws Exception {
            String token = login("user@example.com", "password", "User").refreshToken;

            RestTestClient client1 = restClient;
            RestTestClient client2 = restTestClientFactory.apply(cookieStore);

            int threads = 2;
            var ready = new CountDownLatch(threads);
            var start = new CountDownLatch(1);

            try (var pool = Executors.newFixedThreadPool(threads)) {
                Callable<Void> task1 = () -> {
                    ready.countDown();
                    start.await();
                    client1.post().uri("/auth/logout").exchange().expectStatus().isNoContent();
                    return null;
                };

                Callable<Void> task2 = () -> {
                    ready.countDown();
                    start.await();
                    client2.post().uri("/auth/logout").exchange().expectStatus().isNoContent();
                    return null;
                };

                var f1 = pool.submit(task1);
                var f2 = pool.submit(task2);

                // wait both tasks are ready, then release them together
                assertThat(ready.await(2, TimeUnit.SECONDS)).isTrue();
                start.countDown();

                f1.get(5, TimeUnit.SECONDS);
                f2.get(5, TimeUnit.SECONDS);

            }

            RefreshToken rt = findTokenByHash(token);

            assertThat(rt)
                    .as("Token should be revoked due to logout")
                    .matches(t -> t.getRevokedAt() != null && t.wasLoggedOut());
            assertCookieStoreContainsDeviceId();
        }

    }
}