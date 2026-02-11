package io.github.rivon0507.courier.envoi;

import io.github.rivon0507.courier.common.annotation.CurrentUserId;
import io.github.rivon0507.courier.common.pagination.PagedResponse;
import io.github.rivon0507.courier.envoi.api.EnvoiCreateRequest;
import io.github.rivon0507.courier.envoi.api.EnvoiDetailsResponse;
import io.github.rivon0507.courier.envoi.api.EnvoiResponse;
import io.github.rivon0507.courier.envoi.api.EnvoiUpdateRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/workspaces/{workspaceId}/envois")
@RequiredArgsConstructor
public class EnvoiController {

    private final EnvoiService envoiService;

    @PostMapping
    public ResponseEntity<EnvoiDetailsResponse> create(
            @PathVariable Long workspaceId,
            @Valid @RequestBody EnvoiCreateRequest requestBody,
            @CurrentUserId Long userId) {

        EnvoiDetailsResponse created = envoiService.create(requestBody, workspaceId, userId);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{envoiId}")
                .buildAndExpand(created.envoi().id())
                .toUri();
        return ResponseEntity.created(location)
                .body(created);
    }

    @GetMapping("/{envoiId}")
    public ResponseEntity<EnvoiResponse> get(
            @PathVariable Long envoiId,
            @PathVariable Long workspaceId,
            @CurrentUserId Long userId) {
        return ResponseEntity.ok(envoiService.get(envoiId, workspaceId, userId));
    }

    @GetMapping
    public ResponseEntity<PagedResponse<EnvoiResponse>> getPage(
            @PageableDefault(sort = "dateEnvoi") Pageable page,
            @PathVariable Long workspaceId,
            @CurrentUserId Long userId) {

        return ResponseEntity.ok(envoiService.getPage(page, workspaceId, userId));
    }

    @PutMapping("/{envoiId}")
    public ResponseEntity<EnvoiResponse> update(
            @PathVariable Long envoiId,
            @Valid @RequestBody EnvoiUpdateRequest requestBody,
            @PathVariable Long workspaceId,
            @CurrentUserId Long userId) {

        return ResponseEntity.ok(envoiService.update(envoiId, requestBody, workspaceId, userId));
    }

    @DeleteMapping("/{envoiId}")
    public ResponseEntity<Void> delete(
            @PathVariable Long envoiId,
            @PathVariable Long workspaceId,
            @CurrentUserId Long userId) {
        envoiService.delete(envoiId, workspaceId, userId);
        return ResponseEntity.noContent().build();
    }
}
