package io.github.rivon0507.courier.reception;

import io.github.rivon0507.courier.IntegrationTest;
import io.github.rivon0507.courier.TestUtils;
import io.github.rivon0507.courier.auth.service.AuthService;
import io.github.rivon0507.courier.common.persistence.UserRepository;
import io.github.rivon0507.courier.reception.persistence.ReceptionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.client.RestTestClient;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
public class ReceptionControllerIT {

    @Autowired
    private AuthService authService;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private RestTestClient restClient;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ReceptionRepository receptionRepository;

    private TestUtils.AuthResult auth;

    @BeforeEach
    void setUp() {
        authService.register("user@example.com", "password", "User", "");
        authService.register("newUser@example.com", "password", "User", "");
        auth = TestUtils.login(restClient);
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.execute("TRUNCATE users, refresh_tokens, reception, reception_pieces, workspace RESTART IDENTITY CASCADE");
    }

    @Test
    void create_with_three_pieces_returns_201() {
        restClient.post().uri("/workspaces/%d/receptions".formatted(auth.workspaceId()))
                .header("Authorization", "Bearer %s".formatted(auth.accessToken()))
                .body("""
                        {
                          "dateReception": "2025-12-25",
                          "expediteur": "exp",
                          "reference": "RECEPTION-100",
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
                .jsonPath("$.reception.id").exists()
                .jsonPath("$.reception.reference").isEqualTo("RECEPTION-100")
                .jsonPath("$.reception.dateReception").isEqualTo("2025-12-25")
                .jsonPath("$.reception.expediteur").isEqualTo("exp")
                .jsonPath("$.pieces[0].id").exists()
                .jsonPath("$.pieces[0].designation").isEqualTo("des1")
                .jsonPath("$.pieces[0].quantite").isEqualTo(1);

        assertThat(receptionRepository.count()).as("The reception should have been created").isOne();
        assertThat(receptionRepository.findAll().getFirst().getPieces()).hasSize(3);
    }

    @Test
    void get_page_returns200() {
        restClient.get().uri("/workspaces/%d/receptions".formatted(auth.workspaceId()))
                .header("Authorization", "Bearer %s".formatted(auth.accessToken()))
                .exchange().expectStatus().isOk()
                .expectBody()
                .jsonPath("$._items").isArray()
                .jsonPath("$._page").exists()
                .jsonPath("$._sort").exists();
        TestUtils.createReception(auth, restClient);
        restClient.get().uri("/workspaces/%d/receptions".formatted(auth.workspaceId()))
                .header("Authorization", "Bearer %s".formatted(auth.accessToken()))
                .exchange().expectStatus().isOk()
                .expectBody()
                .jsonPath("$._items").isArray()
                .jsonPath("$._items[0]").exists()
                .jsonPath("$._page").exists()
                .jsonPath("$._sort").exists()
                .jsonPath("$._sort.key").isEqualTo("dateReception");
    }

    @Test
    void get_one_with_nonexistent_reception_id_returns404() {
        restClient.get().uri("/workspaces/%d/receptions/%d".formatted(auth.workspaceId(), 999))
                .header("Authorization", "Bearer %s".formatted(auth.accessToken()))
                .exchange().expectStatus().isNotFound();
    }

    @Test
    void get_one_returns200() {
        long receptionId = TestUtils.createReception(auth, restClient);
        restClient.get().uri("/workspaces/%d/receptions/%d".formatted(auth.workspaceId(), receptionId))
                .header("Authorization", "Bearer %s".formatted(auth.accessToken()))
                .exchange().expectStatus().isOk()
                .expectBody().jsonPath("$.dateReception").isEqualTo("2025-12-25");
    }

    @Test
    void update_alters_the_reception() {
        long receptionId = TestUtils.createReception(auth, restClient);
        restClient.put().uri("/workspaces/%d/receptions/%d".formatted(auth.workspaceId(), receptionId))
                .header("Authorization", "Bearer %s".formatted(auth.accessToken()))
                .body("{\"dateReception\": \"2000-01-01\", \"reference\": \"REF-1\", \"expediteur\": \"EXP\"}")
                .exchange().expectStatus().isOk()
                .expectBody()
                .jsonPath("$.dateReception").isEqualTo("2000-01-01")
                .jsonPath("$.reference").isEqualTo("REF-1")
                .jsonPath("$.expediteur").isEqualTo("EXP");
    }

    @Test
    void delete_existing_deletes_in_DB() {
        long receptionId = TestUtils.createReception(auth, restClient);
        assertThat(receptionRepository.count()).isOne();
        restClient.delete().uri("/workspaces/%d/receptions/%d".formatted(auth.workspaceId(), receptionId))
                .header("Authorization", "Bearer %s".formatted(auth.accessToken()))
                .exchange().expectStatus().isNoContent();
        assertThat(receptionRepository.count()).isZero();
    }

    @Test
    void delete_nonexistent_reception_returns204() {
        TestUtils.createReception(auth, restClient);
        assertThat(receptionRepository.count()).isOne();
        restClient.delete().uri("/workspaces/%d/receptions/%d".formatted(auth.workspaceId(), 999))
                .header("Authorization", "Bearer %s".formatted(auth.accessToken()))
                .exchange().expectStatus().isNoContent();
        assertThat(receptionRepository.count()).isOne();
    }

    @Nested
    class PreAuthorization {
        @Test
        void request_with_workspace_id_not_owned_by_user_returns_404() {
            long notMyWorkspace = userRepository.findUserByEmail("newUser@example.com").orElseThrow().getDefaultWorkspace().getId();
            restClient.get().uri("/workspaces/%d/receptions".formatted(notMyWorkspace))
                    .header("Authorization", "Bearer %s".formatted(auth.accessToken()))
                    .exchange().expectStatus().isNotFound();
        }

        @Test
        void request_with_nonexistent_workspace_id_returns_404() {
            long nonexistentWorkspace = 10000;
            restClient.get().uri("/workspaces/%d/receptions".formatted(nonexistentWorkspace))
                    .header("Authorization", "Bearer %s".formatted(auth.accessToken()))
                    .exchange().expectStatus().isNotFound();
        }

    }
}