package io.github.rivon0507.courier.envoi;

import io.github.rivon0507.courier.common.pagination.PagedResponse;
import io.github.rivon0507.courier.envoi.api.PieceCreateRequest;
import io.github.rivon0507.courier.envoi.api.PieceResponse;
import io.github.rivon0507.courier.envoi.api.PieceUpdateRequest;
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
            @PathVariable String envoiId,
            @RequestBody List<PieceCreateRequest> requestBody,
            @PathVariable Long workspaceId) {

        return ResponseEntity.ok(envoiService.createPieces(envoiId, requestBody, workspaceId));
    }

    @GetMapping
    public ResponseEntity<PagedResponse<PieceResponse>> getPiecesPage(
            @PageableDefault(sort = "designation") Pageable page,
            @PathVariable String envoiId,
            @PathVariable Long workspaceId) {

        return ResponseEntity.ok(envoiService.getPiecesPage(envoiId, page, workspaceId));
    }

    @PutMapping
    public ResponseEntity<List<PieceResponse>> updatePieces(
            @PathVariable String envoiId,
            @RequestBody List<PieceUpdateRequest> requestBody,
            @PathVariable Long workspaceId) {

        return ResponseEntity.ok(envoiService.updatePieces(envoiId, requestBody, workspaceId));
    }

    @DeleteMapping
    public ResponseEntity<Void> deletePieces(
            @PathVariable String envoiId,
            @RequestParam(name = "ids") List<Long> ids,
            @PathVariable Long workspaceId) {

        envoiService.deletePieces(envoiId, ids, workspaceId);
        return ResponseEntity.noContent().build();
    }
}
