package io.github.rivon0507.courier.reception;

import io.github.rivon0507.courier.IntegrationTest;
import io.github.rivon0507.courier.TestUtils;
import io.github.rivon0507.courier.auth.service.AuthService;
import io.github.rivon0507.courier.common.api.PieceCreateRequest;
import io.github.rivon0507.courier.common.persistence.UserRepository;
import io.github.rivon0507.courier.reception.persistence.ReceptionPieceRepository;
import io.github.rivon0507.courier.reception.persistence.ReceptionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
public class ReceptionPiecesControllerIT {
    @Autowired
    private RestTestClient restClient;
    @Autowired
    private AuthService authService;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ReceptionRepository receptionRepository;
    @Autowired
    private ReceptionPieceRepository pieceRepository;

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
    void create_two_pieces_returns_200_and_inserts_in_db() {
        long receptionId = TestUtils.createReception(auth, restClient);
        assertThat(pieceRepository.count()).isZero();
        restClient.post().uri("/workspaces/%d/receptions/%d/pieces".formatted(auth.workspaceId(), receptionId))
                .header("Authorization", "Bearer %s".formatted(auth.accessToken()))
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
        long receptionId = TestUtils.createReception(auth, restClient);
        long before = pieceRepository.count();
        restClient.post().uri("/workspaces/%d/receptions/%d/pieces".formatted(auth.workspaceId(), receptionId))
                .header("Authorization", "Bearer %s".formatted(auth.accessToken()))
                .body("[]")
                .exchange()
                .expectStatus().isOk().expectBody()
                .jsonPath("$").isArray()
                .jsonPath("$.length()").isEqualTo(0);
        assertThat(pieceRepository.count()).isEqualTo(before);
    }

    @Test
    void get_page_with_no_params_returns_only_pieces_of_that_reception() {
        long reception1 = createReceptionWithPieces(List.of(new PieceCreateRequest("p1", 1)));
        long reception2 = createReceptionWithPieces(List.of(new PieceCreateRequest("p2", 1)));
        assertThat(receptionRepository.count()).isEqualTo(2);
        assertThat(pieceRepository.count()).isEqualTo(2);
        restClient.get().uri("/workspaces/%d/receptions/%d/pieces".formatted(auth.workspaceId(), reception1))
                .header("Authorization", "Bearer %s".formatted(auth.accessToken()))
                .exchange().expectStatus().isOk()
                .expectBody()
                .jsonPath("$._items").isArray()
                .jsonPath("$._items.length()").isEqualTo(1)
                .jsonPath("$._items[0].designation").isEqualTo("p1")
                .jsonPath("$._page").exists()
                .jsonPath("$._sort").exists();
        restClient.get()
                .uri("/workspaces/%d/receptions/%d/pieces".formatted(auth.workspaceId(), reception2))
                .header("Authorization", "Bearer %s".formatted(auth.accessToken()))
                .exchange().expectStatus().isOk()
                .expectBody()
                .jsonPath("$._items.length()").isEqualTo(1)
                .jsonPath("$._items[0].designation").isEqualTo("p2");
    }

    @Test
    void update_two_distinct_pieces_returns_200_and_updates_in_db() {
        long receptionId = TestUtils.createReception(auth, restClient);
        ReceptionPiecesControllerIT.PieceIds ids = createPieces(receptionId, List.of(
                new PieceCreateRequest("a", 1),
                new PieceCreateRequest("b", 2)
        ));
        restClient.put().uri("/workspaces/%d/receptions/%d/pieces".formatted(auth.workspaceId(), receptionId))
                .header("Authorization", "Bearer %s".formatted(auth.accessToken()))
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
        long receptionId = TestUtils.createReception(auth, restClient);
        ReceptionPiecesControllerIT.PieceIds ids = createPieces(receptionId, List.of(
                new PieceCreateRequest("a", 1),
                new PieceCreateRequest("b", 2)
        ));
        restClient.put().uri("/workspaces/%d/receptions/%d/pieces".formatted(auth.workspaceId(), receptionId))
                .header("Authorization", "Bearer %s".formatted(auth.accessToken()))
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
        long receptionId = TestUtils.createReception(auth, restClient);
        ReceptionPiecesControllerIT.PieceIds ids = createPieces(receptionId, List.of(
                new PieceCreateRequest("a", 1),
                new PieceCreateRequest("b", 2)
        ));
        restClient.put().uri("/workspaces/%d/receptions/%d/pieces".formatted(auth.workspaceId(), receptionId))
                .header("Authorization", "Bearer %s".formatted(auth.accessToken()))
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
        long receptionId = TestUtils.createReception(auth, restClient);
        createPieces(receptionId, List.of(
                new PieceCreateRequest("a", 1),
                new PieceCreateRequest("b", 2)
        ));

        long before = pieceRepository.count();
        restClient.delete().uri("/workspaces/%d/receptions/%d/pieces?ids=%d".formatted(auth.workspaceId(), receptionId, 999))
                .header("Authorization", "Bearer %s".formatted(auth.accessToken()))
                .exchange().expectStatus().isNoContent();
        assertThat(pieceRepository.count()).isEqualTo(before);
    }

    @Test
    void delete_existing_piece_returns_204_and_deletes_from_db() {
        long receptionId = TestUtils.createReception(auth, restClient);

        ReceptionPiecesControllerIT.PieceIds ids = createPieces(receptionId, List.of(
                new PieceCreateRequest("a", 1),
                new PieceCreateRequest("b", 2)
        ));
        assertThat(pieceRepository.count()).isEqualTo(2);

        restClient.delete().uri("/workspaces/%d/receptions/%d/pieces?ids=%d".formatted(auth.workspaceId(), receptionId, ids.firstId))
                .header("Authorization", "Bearer %s".formatted(auth.accessToken()))
                .exchange().expectStatus().isNoContent();
        assertThat(pieceRepository.existsById(ids.firstId)).isFalse();
        assertThat(pieceRepository.count()).isEqualTo(1);
    }

    private ReceptionPiecesControllerIT.PieceIds createPieces(long receptionId, List<PieceCreateRequest> pieces) {
        String body = pieces.stream()
                .map(p -> "{\"designation\": \"%s\", \"quantite\": %d}".formatted(p.designation(), p.quantite()))
                .collect(Collectors.joining(",", "[", "]"));

        AtomicReference<Integer> id1 = new AtomicReference<>();
        AtomicReference<Integer> id2 = new AtomicReference<>();

        restClient.post().uri("/workspaces/%d/receptions/%d/pieces".formatted(auth.workspaceId(), receptionId))
                .header("Authorization", "Bearer %s".formatted(auth.accessToken()))
                .body(body)
                .exchangeSuccessfully()
                .expectBody()
                .jsonPath("$[0].id").value(id1::set)
                .jsonPath("$[1].id").value(id2::set);

        return new ReceptionPiecesControllerIT.PieceIds(id1.get(), id2.get());
    }

    private long createReceptionWithPieces(List<PieceCreateRequest> pieces) {
        String piecesJson = pieces.stream()
                .map(p -> "{\"designation\": \"%s\", \"quantite\": %d}".formatted(p.designation(), p.quantite()))
                .collect(Collectors.joining(",", "[", "]"));

        AtomicReference<Integer> receptionId = new AtomicReference<>();
        restClient.post().uri("/workspaces/%d/receptions".formatted(auth.workspaceId()))
                .header("Authorization", "Bearer %s".formatted(auth.accessToken()))
                .body("{\"dateReception\": \"2025-12-25\", \"destinataire\": \"dest\", \"pieces\": %s}".formatted(piecesJson))
                .exchangeSuccessfully().expectBody().jsonPath("$.reception.id").value(receptionId::set);

        return receptionId.get();
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
            long receptionId = TestUtils.createReception(auth, restClient);
            restClient.get().uri("/workspaces/%d/receptions/%d/pieces".formatted(notMyWorkspace, receptionId))
                    .header("Authorization", "Bearer %s".formatted(auth.accessToken()))
                    .exchange().expectStatus().isNotFound();
        }

        @Test
        void request_with_nonexistent_workspace_id_returns_404() {
            long nonexistentWorkspace = 10_000L;
            long receptionId = TestUtils.createReception(auth, restClient);
            restClient.get().uri("/workspaces/%d/receptions/%d/pieces".formatted(nonexistentWorkspace, receptionId))
                    .header("Authorization", "Bearer %s".formatted(auth.accessToken()))
                    .exchange().expectStatus().isNotFound();
        }
    }
}
