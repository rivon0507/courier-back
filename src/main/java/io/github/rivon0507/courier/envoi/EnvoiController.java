package io.github.rivon0507.courier.envoi;

import io.github.rivon0507.courier.common.pagination.PagedResponse;
import io.github.rivon0507.courier.envoi.api.EnvoiCreateRequest;
import io.github.rivon0507.courier.envoi.api.EnvoiResponse;
import io.github.rivon0507.courier.envoi.api.EnvoiUpdateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/workspace/{workspaceId}/envoi")
@RequiredArgsConstructor
public class EnvoiController {

    private final EnvoiService envoiService;

    @PostMapping
    public ResponseEntity<EnvoiResponse> post(@PathVariable Long workspaceId, @RequestBody EnvoiCreateRequest requestBody) {
        EnvoiResponse created = envoiService.create(requestBody, workspaceId);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{envoiId}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location)
                .body(created);
    }

    @GetMapping("/{envoiId}")
    public ResponseEntity<EnvoiResponse> get(@PathVariable Long envoiId, @PathVariable Long workspaceId) {
        return ResponseEntity.ok(envoiService.get(envoiId, workspaceId));
    }

    @GetMapping
    public ResponseEntity<PagedResponse<EnvoiResponse>> getPage(
            @PageableDefault(sort = "dateEnvoi", direction = Sort.Direction.DESC) Pageable page,
            @PathVariable Long workspaceId) {

        return ResponseEntity.ok(envoiService.getPage(page, workspaceId));
    }

    @PutMapping("/{envoiId}")
    public ResponseEntity<EnvoiResponse> update(
            @PathVariable String envoiId,
            @RequestBody EnvoiUpdateRequest requestBody,
            @PathVariable Long workspaceId) {

        return ResponseEntity.ok(envoiService.update(envoiId, workspaceId, requestBody));
    }

    @DeleteMapping("/{envoiId}")
    public ResponseEntity<Void> delete(@PathVariable String envoiId, @PathVariable Long workspaceId) {
        envoiService.delete(envoiId, workspaceId);
        return ResponseEntity.noContent().build();
    }
}
