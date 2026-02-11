package io.github.rivon0507.courier.envoi;

import io.github.rivon0507.courier.common.annotation.CurrentUserId;
import io.github.rivon0507.courier.common.pagination.PagedResponse;
import io.github.rivon0507.courier.envoi.api.PieceCreateRequest;
import io.github.rivon0507.courier.envoi.api.PieceResponse;
import io.github.rivon0507.courier.envoi.api.PieceUpdateRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/workspaces/{workspaceId}/envois/{envoiId}/pieces")
@RequiredArgsConstructor
public class PiecesController {

    private final EnvoiService envoiService;

    @PostMapping
    public ResponseEntity<List<PieceResponse>> createPieces(
            @PathVariable Long envoiId,
            @Valid @RequestBody List<PieceCreateRequest> requestBody,
            @PathVariable Long workspaceId,
            @CurrentUserId Long userId) {

        return ResponseEntity.ok(envoiService.createPieces(envoiId, requestBody, workspaceId, userId));
    }

    @GetMapping
    public ResponseEntity<PagedResponse<PieceResponse>> getPiecesPage(
            @PageableDefault(sort = "designation") Pageable page,
            @PathVariable Long envoiId,
            @PathVariable Long workspaceId,
            @CurrentUserId Long userId) {

        return ResponseEntity.ok(envoiService.getPiecesPage(envoiId, page, workspaceId, userId));
    }

    @PutMapping
    public ResponseEntity<List<PieceResponse>> updatePieces(
            @PathVariable Long envoiId,
            @Valid @RequestBody List<PieceUpdateRequest> requestBody,
            @PathVariable Long workspaceId,
            @CurrentUserId Long userId) {

        return ResponseEntity.ok(envoiService.updatePieces(envoiId, requestBody, workspaceId, userId));
    }

    @DeleteMapping
    public ResponseEntity<Void> deletePieces(
            @PathVariable Long envoiId,
            @RequestParam(name = "ids") List<Long> ids,
            @PathVariable Long workspaceId,
            @CurrentUserId Long userId) {

        envoiService.deletePieces(envoiId, ids, workspaceId, userId);
        return ResponseEntity.noContent().build();
    }
}
