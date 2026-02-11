package io.github.rivon0507.courier.reception;

import io.github.rivon0507.courier.common.annotation.CurrentUserId;
import io.github.rivon0507.courier.common.pagination.PagedResponse;
import io.github.rivon0507.courier.reception.api.ReceptionCreateRequest;
import io.github.rivon0507.courier.reception.api.ReceptionDetailsResponse;
import io.github.rivon0507.courier.reception.api.ReceptionResponse;
import io.github.rivon0507.courier.reception.api.ReceptionUpdateRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/workspaces/{workspaceId}/receptions")
@RequiredArgsConstructor
class ReceptionController {

    private final ReceptionService receptionService;

    @PostMapping
    public ResponseEntity<ReceptionDetailsResponse> create(
            @PathVariable Long workspaceId,
            @Valid @RequestBody ReceptionCreateRequest requestBody,
            @CurrentUserId Long userId) {

        ReceptionDetailsResponse created = receptionService.create(requestBody, workspaceId, userId);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{receptionId}")
                .buildAndExpand(created.reception().id())
                .toUri();
        return ResponseEntity.created(location)
                .body(created);
    }

    @GetMapping("/{receptionId}")
    public ResponseEntity<ReceptionResponse> get(
            @PathVariable Long receptionId,
            @PathVariable Long workspaceId,
            @CurrentUserId Long userId) {
        return ResponseEntity.ok(receptionService.get(receptionId, workspaceId, userId));
    }

    @GetMapping
    public ResponseEntity<PagedResponse<ReceptionResponse>> getPage(
            @PageableDefault(sort = "dateReception") Pageable page,
            @PathVariable Long workspaceId,
            @CurrentUserId Long userId) {

        return ResponseEntity.ok(receptionService.getPage(page, workspaceId, userId));
    }

    @PutMapping("/{receptionId}")
    public ResponseEntity<ReceptionResponse> update(
            @PathVariable Long receptionId,
            @Valid @RequestBody ReceptionUpdateRequest requestBody,
            @PathVariable Long workspaceId,
            @CurrentUserId Long userId) {

        return ResponseEntity.ok(receptionService.update(receptionId, requestBody, workspaceId, userId));
    }

    @DeleteMapping("/{receptionId}")
    public ResponseEntity<Void> delete(
            @PathVariable Long receptionId,
            @PathVariable Long workspaceId,
            @CurrentUserId Long userId) {
        receptionService.delete(receptionId, workspaceId, userId);
        return ResponseEntity.noContent().build();
    }
}
