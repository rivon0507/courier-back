package io.github.rivon0507.courier;

import org.jspecify.annotations.NonNull;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class TestUtils {
    public static AuthResult login(RestTestClient restClient) {
        AtomicReference<String> accessToken = new AtomicReference<>();
        AtomicReference<Integer> workspaceId = new AtomicReference<>();
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

    public static long createEnvoi(@NonNull AuthResult auth, RestTestClient restClient) {
        AtomicReference<Integer> envoiId = new AtomicReference<>();
        restClient.post().uri("/workspaces/%d/envois".formatted(auth.workspaceId()))
                .header("Authorization", "Bearer %s".formatted(auth.accessToken()))
                .body("{\"dateEnvoi\": \"2025-12-25\", \"destinataire\": \"dest\"}")
                .exchangeSuccessfully()
                .expectBody().jsonPath("$.envoi.id").value(envoiId::set);
        return envoiId.get();
    }

    public static long createReception(AuthResult auth, RestTestClient restClient) {
        AtomicReference<Integer> receptionId = new AtomicReference<>();
        restClient.post().uri("/workspaces/%d/receptions".formatted(auth.workspaceId()))
                .header("Authorization", "Bearer %s".formatted(auth.accessToken()))
                .body("{\"dateReception\": \"2025-12-25\", \"expediteur\": \"exp\", \"reference\": \"REF\"}")
                .exchangeSuccessfully()
                .expectBody().jsonPath("$.reception.id").value(receptionId::set);
        return receptionId.get();
    }

    public record AuthResult(String accessToken, long workspaceId) {
    }
}
