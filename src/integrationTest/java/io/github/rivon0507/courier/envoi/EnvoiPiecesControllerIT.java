package io.github.rivon0507.courier.envoi;

import io.github.rivon0507.courier.IntegrationTest;
import io.github.rivon0507.courier.auth.service.AuthService;
import io.github.rivon0507.courier.common.persistence.UserRepository;
import io.github.rivon0507.courier.envoi.persistence.EnvoiPieceRepository;
import io.github.rivon0507.courier.envoi.persistence.EnvoiRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
public class EnvoiPiecesControllerIT {

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
    @Autowired
    private EnvoiPieceRepository pieceRepository;

    private AuthResult auth;

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
    void create_two_pieces_returns_200_and_inserts_in_db() {
        long envoiId = createEnvoi();
        assertThat(pieceRepository.count()).isZero();
        restClient.post().uri("/workspaces/%d/envois/%d/pieces".formatted(auth.workspaceId, envoiId))
                .header("Authorization", "Bearer %s".formatted(auth.accessToken))
                .body("""
                        [
                          {"designation": "other", "quantite": 5},
                          {"designation": "design", "quantite": 1}
                        ]
                        """)
                .exchange().expectStatus().isOk()
                .expectBody()
                .jsonPath("$").isArray()
                .jsonPath("$[0].id").exists()
                .jsonPath("$[0].designation").isEqualTo("other")
                .jsonPath("$[0].quantite").isEqualTo(5)
                .jsonPath("$[1].id").exists()
                .jsonPath("$[1].designation").isEqualTo("design")
                .jsonPath("$[1].quantite").isEqualTo(1);
        assertThat(pieceRepository.count()).as("Two pieces should have been inserted").isEqualTo(2);
    }

    @Test
    void create_empty_list_returns_200_and_inserts_nothing() {
        long envoiId = createEnvoi();
        long before = pieceRepository.count();
        restClient.post().uri("/workspaces/%d/envois/%d/pieces".formatted(auth.workspaceId, envoiId))
                .header("Authorization", "Bearer %s".formatted(auth.accessToken))
                .body("[]")
                .exchange()
                .expectStatus().isOk().expectBody()
                .jsonPath("$").isArray()
                .jsonPath("$.length()").isEqualTo(0);
        assertThat(pieceRepository.count()).isEqualTo(before);
    }

    @Test
    void get_page_with_no_params_returns_only_pieces_of_that_envoi() {
        long envoi1 = createEnvoiWithPieces(List.of(new PieceSeed("p1", 1)));
        long envoi2 = createEnvoiWithPieces(List.of(new PieceSeed("p2", 1)));
        assertThat(envoiRepository.count()).isEqualTo(2);
        assertThat(pieceRepository.count()).isEqualTo(2);
        restClient.get().uri("/workspaces/%d/envois/%d/pieces".formatted(auth.workspaceId, envoi1))
                .header("Authorization", "Bearer %s".formatted(auth.accessToken))
                .exchange().expectStatus().isOk()
                .expectBody()
                .jsonPath("$._items").isArray()
                .jsonPath("$._items.length()").isEqualTo(1)
                .jsonPath("$._items[0].designation").isEqualTo("p1")
                .jsonPath("$._page").exists()
                .jsonPath("$._sort").exists();
        restClient.get()
                .uri("/workspaces/%d/envois/%d/pieces".formatted(auth.workspaceId, envoi2))
                .header("Authorization", "Bearer %s".formatted(auth.accessToken))
                .exchange().expectStatus().isOk()
                .expectBody()
                .jsonPath("$._items.length()").isEqualTo(1)
                .jsonPath("$._items[0].designation").isEqualTo("p2");
    }

    @Test
    void update_two_distinct_pieces_returns_200_and_updates_in_db() {
        long envoiId = createEnvoi();
        PieceIds ids = createPieces(envoiId, List.of(
                new PieceSeed("a", 1),
                new PieceSeed("b", 2)
        ));
        restClient.put().uri("/workspaces/%d/envois/%d/pieces".formatted(auth.workspaceId, envoiId))
                .header("Authorization", "Bearer %s".formatted(auth.accessToken))
                .body("""
                        [
                          {"id": %d, "designation": "a-up", "quantite": 10},
                          {"id": %d, "designation": "b-up", "quantite": 20}
                        ]
                        """.formatted(ids.firstId, ids.secondId))
                .exchange().expectStatus().isOk();

        var p1 = pieceRepository.findById(ids.firstId).orElseThrow();
        var p2 = pieceRepository.findById(ids.secondId).orElseThrow();

        assertThat(p1.getDesignation()).isEqualTo("a-up");
        assertThat(p1.getQuantite()).isEqualTo(10);
        assertThat(p2.getDesignation()).isEqualTo("b-up");
        assertThat(p2.getQuantite()).isEqualTo(20);
    }

    @Test
    void update_with_duplicate_piece_id_returns_400_DUPLICATE_PIECE_ID() {
        long envoiId = createEnvoi();
        PieceIds ids = createPieces(envoiId, List.of(
                new PieceSeed("a", 1),
                new PieceSeed("b", 2)
        ));
        restClient.put().uri("/workspaces/%d/envois/%d/pieces".formatted(auth.workspaceId, envoiId))
                .header("Authorization", "Bearer %s".formatted(auth.accessToken))
                .body("""
                        [
                          {"id": %d, "designation": "x", "quantite": 10},
                          {"id": %d, "designation": "y", "quantite": 20},
                          {"id": %d, "designation": "dup", "quantite": 30}
                        ]
                        """.formatted(ids.firstId, ids.secondId, ids.firstId))
                .exchange().expectStatus().isBadRequest()
                .expectBody().jsonPath("$.code").isEqualTo("DUPLICATE_PIECE_ID");
    }

    @Test
    void update_with_one_valid_and_one_invalid_piece_returns_400() {
        long envoiId = createEnvoi();
        PieceIds ids = createPieces(envoiId, List.of(
                new PieceSeed("a", 1),
                new PieceSeed("b", 2)
        ));
        restClient.put().uri("/workspaces/%d/envois/%d/pieces".formatted(auth.workspaceId, envoiId))
                .header("Authorization", "Bearer %s".formatted(auth.accessToken))
                .body("""
                        [
                          {"id": %d, "designation": "a-up", "quantite": 10},
                          {"id": %d, "designation": "b-up", "quantite": -1}
                        ]
                        """.formatted(ids.firstId, ids.secondId))
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void delete_nonexistent_piece_returns_204() {
        long envoiId = createEnvoi();
        createPieces(envoiId, List.of(
                new PieceSeed("a", 1),
                new PieceSeed("b", 2)
        ));

        long before = pieceRepository.count();
        restClient.delete().uri("/workspaces/%d/envois/%d/pieces?ids=%d".formatted(auth.workspaceId, envoiId, 999))
                .header("Authorization", "Bearer %s".formatted(auth.accessToken))
                .exchange().expectStatus().isNoContent();
        assertThat(pieceRepository.count()).isEqualTo(before);
    }

    @Test
    void delete_existing_piece_returns_204_and_deletes_from_db() {
        long envoiId = createEnvoi();

        PieceIds ids = createPieces(envoiId, List.of(
                new PieceSeed("a", 1),
                new PieceSeed("b", 2)
        ));
        assertThat(pieceRepository.count()).isEqualTo(2);

        restClient.delete().uri("/workspaces/%d/envois/%d/pieces?ids=%d".formatted(auth.workspaceId, envoiId, ids.firstId))
                .header("Authorization", "Bearer %s".formatted(auth.accessToken))
                .exchange().expectStatus().isNoContent();
        assertThat(pieceRepository.existsById(ids.firstId)).isFalse();
        assertThat(pieceRepository.count()).isEqualTo(1);
    }

    private long createEnvoi() {
        AtomicReference<Integer> envoiId = new AtomicReference<>();
        restClient.post().uri("/workspaces/%d/envois".formatted(auth.workspaceId))
                .header("Authorization", "Bearer %s".formatted(auth.accessToken))
                .body("{\"dateEnvoi\": \"2025-12-25\", \"destinataire\": \"dest\"}")
                .exchangeSuccessfully()
                .expectBody()
                .jsonPath("$.envoi.id").value(envoiId::set);
        return envoiId.get();
    }

    private long createEnvoiWithPieces(List<PieceSeed> pieces) {
        String piecesJson = pieces.stream()
                .map(p -> "{\"designation\": \"%s\", \"quantite\": %d}".formatted(p.designation, p.quantite))
                .collect(Collectors.joining(",", "[", "]"));

        AtomicReference<Integer> envoiId = new AtomicReference<>();
        restClient.post().uri("/workspaces/%d/envois".formatted(auth.workspaceId))
                .header("Authorization", "Bearer %s".formatted(auth.accessToken))
                .body("{\"dateEnvoi\": \"2025-12-25\", \"destinataire\": \"dest\", \"pieces\": %s}".formatted(piecesJson))
                .exchangeSuccessfully().expectBody().jsonPath("$.envoi.id").value(envoiId::set);

        return envoiId.get();
    }

    private PieceIds createPieces(long envoiId, List<PieceSeed> pieces) {
        String body = pieces.stream()
                .map(p -> "{\"designation\": \"%s\", \"quantite\": %d}".formatted(p.designation, p.quantite))
                .collect(Collectors.joining(",", "[", "]"));

        AtomicReference<Integer> id1 = new AtomicReference<>();
        AtomicReference<Integer> id2 = new AtomicReference<>();

        restClient.post().uri("/workspaces/%d/envois/%d/pieces".formatted(auth.workspaceId, envoiId))
                .header("Authorization", "Bearer %s".formatted(auth.accessToken))
                .body(body)
                .exchangeSuccessfully()
                .expectBody()
                .jsonPath("$[0].id").value(id1::set)
                .jsonPath("$[1].id").value(id2::set);

        return new PieceIds(id1.get(), id2.get());
    }

    private AuthResult login() {
        AtomicReference<String> accessToken = new AtomicReference<>();
        AtomicReference<Integer> workspaceId = new AtomicReference<>();
        restClient.post()
                .uri("/auth/login")
                .body(Map.of("email", "user@example.com", "password", "password"))
                .exchangeSuccessfully()
                .expectBody()
                .jsonPath("$.accessToken").value(accessToken::set)
                .jsonPath("$.workspaceId").value(workspaceId::set);

        return new AuthResult(accessToken.get(), workspaceId.get());
    }

    record AuthResult(String accessToken, long workspaceId) {
    }

    record PieceSeed(String designation, int quantite) {
    }

    record PieceIds(long firstId, long secondId) {
    }

    @Nested
    class PreAuthorization {

        @Test
        void request_with_workspace_id_not_owned_by_user_returns_404() {
            long notMyWorkspace = userRepository.findUserByEmail("newUser@example.com")
                    .orElseThrow()
                    .getDefaultWorkspace()
                    .getId();
            long envoiId = createEnvoi();
            restClient.get().uri("/workspaces/%d/envois/%d/pieces".formatted(notMyWorkspace, envoiId))
                    .header("Authorization", "Bearer %s".formatted(auth.accessToken))
                    .exchange().expectStatus().isNotFound();
        }

        @Test
        void request_with_nonexistent_workspace_id_returns_404() {
            long nonexistentWorkspace = 10_000L;
            long envoiId = createEnvoi();
            restClient.get().uri("/workspaces/%d/envois/%d/pieces".formatted(nonexistentWorkspace, envoiId))
                    .header("Authorization", "Bearer %s".formatted(auth.accessToken))
                    .exchange().expectStatus().isNotFound();
        }
    }
}
