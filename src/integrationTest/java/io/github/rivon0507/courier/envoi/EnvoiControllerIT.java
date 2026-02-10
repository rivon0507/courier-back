package io.github.rivon0507.courier.envoi;

import io.github.rivon0507.courier.IntegrationTest;
import io.github.rivon0507.courier.auth.service.AuthService;
import io.github.rivon0507.courier.common.persistence.UserRepository;
import io.github.rivon0507.courier.envoi.persistence.EnvoiRepository;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
public class EnvoiControllerIT {
    @Autowired
    private RestTestClient restClient;
    @Autowired
    private AuthService authService;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private EnvoiRepository envoiRepository;

    private @NonNull AuthResult auth;

    @BeforeEach
    void setUp() {
        authService.register("user@example.com", "password", "User", "");
        authService.register("newUser@example.com", "password", "User", "");
        auth = login();
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.execute("TRUNCATE users, refresh_tokens, envoi, envoi_pieces, workspace RESTART IDENTITY CASCADE");
    }

    @Test
    void create_with_no_reference_and_three_pieces_returns_201() {
        restClient.post().uri("/workspaces/%d/envois".formatted(auth.workspaceId))
                .header("Authorization", "Bearer %s".formatted(auth.accessToken))
                .body("""
                        {
                          "dateEnvoi": "2025-12-25",
                          "destinataire": "dest",
                          "pieces": [
                            {
                              "designation": "des1",
                              "quantite": 1
                            },
                            {
                              "designation": "des2",
                              "quantite": 2
                            },
                            {
                              "designation": "des3",
                              "quantite": 10
                            }
                          ]
                        }""")
                .exchange().expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.envoi.id").exists()
                .jsonPath("$.envoi.reference").isEqualTo("ENVOI-1")
                .jsonPath("$.envoi.dateEnvoi").isEqualTo("2025-12-25")
                .jsonPath("$.envoi.destinataire").isEqualTo("dest")
                .jsonPath("$.pieces[0].id").exists()
                .jsonPath("$.pieces[0].designation").isEqualTo("des1")
                .jsonPath("$.pieces[0].quantite").isEqualTo(1);

        assertThat(envoiRepository.count()).as("The envoi should have been created").isOne();
    }

    @Test
    void create_reference_is_ignored_if_provided() {
        restClient.post().uri("/workspaces/%d/envois".formatted(auth.workspaceId))
                .header("Authorization", "Bearer %s".formatted(auth.accessToken))
                .body("""
                        {
                          "dateEnvoi": "2025-12-25",
                          "destinataire": "dest",
                          "reference": "REF-22"
                        }""")
                .exchangeSuccessfully()
                .expectBody().jsonPath("$.envoi.reference").isEqualTo("ENVOI-1");
    }

    @Test
    void get_page_returns200() {
        restClient.get().uri("/workspaces/%d/envois".formatted(auth.workspaceId))
                .header("Authorization", "Bearer %s".formatted(auth.accessToken))
                .exchange().expectStatus().isOk()
                .expectBody()
                .jsonPath("$._items").isArray()
                .jsonPath("$._page").exists()
                .jsonPath("$._sort").exists();
        createEnvoi();
        restClient.get().uri("/workspaces/%d/envois".formatted(auth.workspaceId))
                .header("Authorization", "Bearer %s".formatted(auth.accessToken))
                .exchange().expectStatus().isOk()
                .expectBody()
                .jsonPath("$._items").isArray()
                .jsonPath("$._items[0]").exists()
                .jsonPath("$._page").exists()
                .jsonPath("$._sort").exists();
    }

    @Test
    void get_one_with_nonexistent_envoi_id_returns404() {
        restClient.get().uri("/workspaces/%d/envois".formatted(auth.workspaceId))
                .header("Authorization", "Bearer %s".formatted(auth.accessToken))
                .exchange().expectStatus().isNotFound();
    }

    @Test
    void get_one_returns200() {
        long envoiId = createEnvoi();
        restClient.get().uri("/workspaces/%d/envois/%d".formatted(auth.workspaceId, envoiId))
                .header("Authorization", "Bearer %s".formatted(auth.accessToken))
                .exchange().expectStatus().isOk()
                .expectBody().jsonPath("$.dateEnvoi").isEqualTo("2025-12-25");
    }

    @Test
    void update_alters_the_envoi() {
        long envoiId = createEnvoi();
        restClient.put().uri("/workspaces/%d/envois/%d".formatted(auth.workspaceId, envoiId))
                .header("Authorization", "Bearer %s".formatted(auth.accessToken))
                .body("{\"dateEnvoi\": \"2000-01-01\", \"observation\": \"obs\", \"destinataire\": \"dest\"}")
                .exchange().expectStatus().isOk()
                .expectBody()
                .jsonPath("$.dateEnvoi").isEqualTo("2000-01-01")
                .jsonPath("$.observation").isEqualTo("obs")
                .jsonPath("$.destinataire").isEqualTo("dest");
    }

    @Test
    void update_ignores_reference_if_provided() {
        long envoiId = createEnvoi();
        restClient.put().uri("/workspaces/%d/envois/%d".formatted(auth.workspaceId, envoiId))
                .header("Authorization", "Bearer %s".formatted(auth.accessToken))
                .body("{\"reference\": \"REF-2000\", \"observation\": \"obs\", \"destinataire\": \"dest\"}")
                .exchange().expectStatus().isOk()
                .expectBody()
                .jsonPath("$.reference").isEqualTo("ENVOI-1")
                .jsonPath("$.observation").isEqualTo("obs");
    }

    @Test
    void delete_existing_deletes_in_DB() {
        long envoiId = createEnvoi();
        assertThat(envoiRepository.count()).isOne();
        restClient.delete().uri("/workspaces/%d/envois/%d".formatted(auth.workspaceId, envoiId))
                .header("Authorization", "Bearer %s".formatted(auth.accessToken))
                .exchange().expectStatus().isNoContent();
        assertThat(envoiRepository.count()).isZero();
    }

    @Test
    void delete_nonexistent_envoi_returns204() {
        createEnvoi();
        assertThat(envoiRepository.count()).isOne();
        restClient.delete().uri("/workspaces/%d/envois/%d".formatted(auth.workspaceId, 999))
                .header("Authorization", "Bearer %s".formatted(auth.accessToken))
                .exchange().expectStatus().isNoContent();
        assertThat(envoiRepository.count()).isOne();
    }

    private long createEnvoi() {
        AtomicReference<Long> envoiId = new AtomicReference<>();
        restClient.post().uri("/workspaces/%d/envois".formatted(auth.workspaceId))
                .header("Authorization", "Bearer %s".formatted(auth.accessToken))
                .body("{\"dateEnvoi\": \"2025-12-25\", \"destinataire\": \"dest\"}")
                .exchangeSuccessfully()
                .expectBody().jsonPath("$.envoi.id").value(envoiId::set);
        return envoiId.get();
    }

    private AuthResult login() {
        AtomicReference<String> accessToken = new AtomicReference<>();
        AtomicReference<Long> workspaceId = new AtomicReference<>();
        restClient.post().uri("/auth/login")
                .body(Map.of("email", "user@example.com", "password", "password"))
                .exchangeSuccessfully()
                .expectBody()
                .jsonPath("$.accessToken").exists()
                .jsonPath("$.accessToken").value(accessToken::set)
                .jsonPath("$.workspaceId").exists()
                .jsonPath("$.workspaceId").value(workspaceId::set);
        return new AuthResult(accessToken.get(), workspaceId.get());
    }

    record AuthResult(String accessToken, long workspaceId) {
    }

    @Nested
    class PreAuthorization {
        @Test
        void request_with_workspace_id_not_owned_by_user_returns_404() {
            long notMyWorkspace = userRepository.findUserByEmail("newUser@example.com").orElseThrow().getDefaultWorkspace().getId();
            restClient.get().uri("/workspaces/%d/envois".formatted(notMyWorkspace))
                    .header("Authorization", "Bearer %s".formatted(auth.accessToken))
                    .exchange().expectStatus().isNotFound();
        }

        @Test
        void request_with_nonexistent_workspace_id_returns_404() {
            long nonexistentWorkspace = 10000;
            restClient.get().uri("/workspaces/%d/envois".formatted(nonexistentWorkspace))
                    .header("Authorization", "Bearer %s".formatted(auth.accessToken))
                    .exchange().expectStatus().isNotFound();
        }
    }
}